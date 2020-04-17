package d4s.models

import cats.MonadError
import cats.effect.concurrent.Ref
import d4s.DynamoConnector.DynamoException
import d4s.DynamoExecutionContext
import d4s.models.ExecutionStrategy.{FThrowable, StreamFThrowable, UnknownF}
import d4s.models.query.DynamoRequest.{PageableRequest, WithLimit, WithProjectionExpression, WithSelect, WithTableReference}
import d4s.models.query.requests._
import d4s.models.query.{DynamoQuery, DynamoRequest}
import d4s.models.table.{TableDDL, TableReference}
import d4s.util.OffsetLimit
import fs2.Stream
import izumi.functional.bio.BIOMonadError
import izumi.functional.bio.catz._
import software.amazon.awssdk.services.dynamodb.model.{ConditionalCheckFailedException, CreateTableResponse, ResourceInUseException, ResourceNotFoundException}

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.language.reflectiveCalls

final case class DynamoExecution[DR <: DynamoRequest, Dec, +A](
  dynamoQuery: DynamoQuery[DR, Dec],
  executionStrategy: ExecutionStrategy[DR, Dec, A]
) extends DynamoExecution.Dependent[DR, Dec, FThrowable[?[_, `+_`], A]] {

  def map[B](f: A => B): DynamoExecution[DR, Dec, B] = {
    modifyExecution(io => _.F.map(io)(f))
  }
  def flatMap[B](f: A => DynamoExecutionContext[UnknownF] => UnknownF[Throwable, B]): DynamoExecution[DR, Dec, B] = {
    modifyExecution(io => ctx => ctx.F.flatMap(io)(f(_)(ctx)))
  }
  def void: DynamoExecution[DR, Dec, Unit] = {
    map(_ => ())
  }
  def redeem[B](err: Throwable => DynamoExecutionContext[UnknownF] => UnknownF[Throwable, B],
                succ: A => DynamoExecutionContext[UnknownF] => UnknownF[Throwable, B]): DynamoExecution[DR, Dec, B] = {
    modifyExecution(io => ctx => ctx.F.redeem(io)(err(_)(ctx), succ(_)(ctx)))
  }
  def catchAll[A1 >: A](err: Throwable => DynamoExecutionContext[UnknownF] => UnknownF[Throwable, A1]): DynamoExecution[DR, Dec, A1] = {
    redeem(err, a => _.F.pure(a))
  }

  def retryWithPrefix(ddl: TableDDL, sleep: Duration = 1.second)(implicit ev: DR <:< WithTableReference[DR]): DynamoExecution[DR, Dec, A] = {
    modifyStrategy(DynamoExecution.retryWithPrefix(ddl, sleep))
  }

  def optConditionFailure: DynamoExecution[DR, Dec, Option[ConditionalCheckFailedException]] = {
    redeem({
      case err: ConditionalCheckFailedException => _.F.pure(Some(err))
      case err: Throwable                       => _.F.fail(err)
    }, _ => _.F.pure(None))
  }

  def boolConditionSuccess: DynamoExecution[DR, Dec, Boolean] = {
    optConditionFailure.map(_.isEmpty)
  }

  def modify(f: DR => DR): DynamoExecution[DR, Dec, A] = {
    copy(dynamoQuery = dynamoQuery.modify(f))
  }
  def modifyQuery(f: DynamoQuery[DR, Dec] => DynamoQuery[DR, Dec]): DynamoExecution[DR, Dec, A] = {
    copy(dynamoQuery = f(dynamoQuery))
  }
  def modifyStrategy[Dec1 >: Dec, B](f: ExecutionStrategy[DR, Dec, A] => ExecutionStrategy[DR, Dec1, B]): DynamoExecution[DR, Dec1, B] = {
    copy(executionStrategy = f(executionStrategy))
  }
  def modifyExecution[B](f: UnknownF[Throwable, A] => DynamoExecutionContext[UnknownF] => UnknownF[Throwable, B]): DynamoExecution[DR, Dec, B] = {
    copy(executionStrategy = ExecutionStrategy[DR, Dec, B] {
      query => ctx =>
        f(executionStrategy(query)(ctx))(ctx)
    })
  }

}

object DynamoExecution {

  def apply[DR <: DynamoRequest, Dec](dynamoQuery: DynamoQuery[DR, Dec]): DynamoExecution[DR, Dec, Dec] = {
    new DynamoExecution[DR, Dec, Dec](dynamoQuery, DynamoExecution.single[DR, Dec])
  }

  def single[DR <: DynamoRequest, Dec]: ExecutionStrategy[DR, Dec, Dec] = {
    ExecutionStrategy {
      query => ctx =>
        import ctx._
        ctx.interpreter
          .run(query)
          .flatMap(query.decoder(_))
    }
  }

  def createTable[F[+_, +_]](table: TableReference, ddl: TableDDL, sleep: Duration = 1.second): DynamoExecution[CreateTable, CreateTableResponse, Unit] = {
    CreateTable(table, ddl).toQuery.exec.redeem(
      {
        case _: ResourceInUseException => _.F.unit
        case e                         => _.F.fail(e)
      }, {
        rsp => ctx =>
          import ctx._

          val updateTTL = table.ttlField match {
            case None    => F.unit
            case Some(_) => interpreter.run(DynamoQuery(UpdateTTL(table))).void
          }
          val tagResources = F.when(table.tags.nonEmpty) {
            interpreter.run(DynamoQuery(UpdateTableTags(table, rsp.tableDescription().tableArn()))).void
          }
          val updateContinuousBackups = ddl.backupEnabled match {
            case Some(true) => interpreter.run(DynamoQuery(UpdateContinuousBackups(table, backupEnabled = true)))
            case _          => F.unit
          }

          // wait until the table appears
          retryIfTableNotFound(attempts = 120, F.sleep(sleep).widenError[Throwable])(F.unit) {
            updateTTL *> tagResources *> updateContinuousBackups.void
          }
      }
    )
  }

  def listTables: DynamoExecution[ListTables, List[String], List[String]] = {
    ListTables().toQuery.decode(_.tableNames().asScala.toList).execPagedFlatten()
  }

  def listTablesStream: DynamoExecution.Streamed[ListTables, List[String], String] = {
    ListTables().toQuery.decode(_.tableNames().asScala.toList).execStreamedFlatten
  }

  def offset[DR <: DynamoRequest, Dec, A](offsetLimit: OffsetLimit)(
    implicit
    paging: PageableRequest[DR],
    ev1: DR <:< WithSelect[DR] with WithLimit[DR] with WithProjectionExpression[DR],
    ev2: DR#Rsp => { def count(): Integer },
    ev3: Dec <:< List[A]
  ): ExecutionStrategy[DR, Dec, List[A]] = ExecutionStrategy {
    query => ctx =>
      import ctx._
      import paging.PageMarker

      def firstOffsetKey(): F[Throwable, Option[PageMarker]] = {
        def go(lastEvaluatedKey: Option[PageMarker], fetched: Int): F[Throwable, Option[PageMarker]] = {
          lastEvaluatedKey match {
            case None                               => F.pure(lastEvaluatedKey)
            case _ if fetched == offsetLimit.offset => F.pure(lastEvaluatedKey)
            case Some(nextPage) =>
              val newRq = query.modify(paging.withPageMarker(_, nextPage).withLimit(offsetLimit.offset - fetched))
              interpreter
                .run(newRq)
                .flatMap(newRsp => go(paging.getPageMarker(newRsp), fetched + newRsp.count()))
          }
        }

        for {
          skipOffsetRsp <- interpreter.run(query.withLimit(offsetLimit.offset).countOnly)
          res           <- go(paging.getPageMarker(skipOffsetRsp), skipOffsetRsp.count())
        } yield res
      }

      if (offsetLimit.offset <= 0) {
        pagedFlatten[DR, Dec, A](Some(offsetLimit.limit.toInt)).apply(query)(ctx)
      } else {
        for {
          lastKey <- firstOffsetKey()
          newReq = query.modify {
            rq =>
              lastKey
                .fold(rq)(paging.withPageMarker(rq, _))
                .withLimit(offsetLimit.limit.toInt)
          }
          res <- pagedFlatten[DR, Dec, A](Some(offsetLimit.limit.toInt)).apply(newReq)(ctx)
        } yield res
      }
  }

  def pagedFlatten[DR <: DynamoRequest: PageableRequest, Dec: ? <:< List[A], A](limit: Option[Int] = None): ExecutionStrategy[DR, Dec, List[A]] = {
    pagedImpl[DR, Dec, A](limit)(_.flatten)
  }

  def paged[DR <: DynamoRequest: PageableRequest, Dec](limit: Option[Int] = None): ExecutionStrategy[DR, Dec, List[Dec]] = {
    pagedImpl(limit)(identity(_))
  }

  private[this] def pagedImpl[DR <: DynamoRequest, Dec, A](
    limit: Option[Int]
  )(f: List[Dec] => List[A])(
    implicit paging: PageableRequest[DR]
  ): ExecutionStrategy[DR, Dec, List[A]] =
    ExecutionStrategy {
      req => ctx =>
        import ctx._

        def go(rsp: DR#Rsp, rows: Queue[Dec]): F[Throwable, List[A]] = {
          val lastEvaluatedKey = paging.getPageMarker(rsp)
          def stop = {
            val res = f(rows.toList)
            F.pure(limit.fold(res)(res.take))
          }

          lastEvaluatedKey match {
            case None                              => stop
            case _ if limit.exists(_ <= rows.size) => stop
            case Some(nextPage) =>
              val newReq = req.modify(paging.withPageMarker(_, nextPage))
              (for {
                newRsp  <- interpreter.run(newReq)
                decoded <- req.decoder[F](newRsp)
              } yield go(newRsp, rows :+ decoded)).flatMap(identity)
          }
        }

        for {
          newRsp  <- interpreter.run(req)
          decoded <- req.decoder[F](newRsp)
          res     <- go(newRsp, Queue(decoded))
        } yield res
    }

  def retryWithPrefix[DR <: DynamoRequest, Dec, A](ddl: TableDDL, sleep: Duration = 1.second)(nested: ExecutionStrategy[DR, Dec, A])(
    implicit
    ev: DR <:< WithTableReference[DR]
  ): ExecutionStrategy[DR, Dec, A] = ExecutionStrategy[DR, Dec, A] {
    req => ctx =>
      import ctx._

      val newTableReq = DynamoExecution.createTable(req.table, ddl)
      val mkTable     = newTableReq.executionStrategy(newTableReq.dynamoQuery)(ctx)

      retryIfTableNotFound[F[Throwable, ?], A](attempts = 120, F.sleep(sleep))(mkTable) {
        nested(req)(ctx)
      }
  }

  private[this] def retryIfTableNotFound[F[_], A](attempts: Int,
                                                  sleep: F[Unit])(prepareTable: F[Unit])(attemptAction: F[A])(implicit F: MonadError[F, Throwable]): F[A] = {
    import cats.syntax.applicativeError._
    import cats.syntax.flatMap._

    attemptAction.handleErrorWith {
      case e @ (_: ResourceNotFoundException | _: ResourceInUseException | DynamoException(_, _: ResourceNotFoundException) |
          DynamoException(_, _: ResourceInUseException)) =>
        if (attempts > 0) {
          prepareTable >>
          sleep >>
          retryIfTableNotFound(attempts - 1, sleep)(prepareTable)(attemptAction)
        } else {
          F.raiseError(e)
        }
      case e =>
        F.raiseError(e)
    }
  }

  final case class Streamed[DR <: DynamoRequest, Dec, +A](
    dynamoQuery: DynamoQuery[DR, Dec],
    executionStrategy: ExecutionStrategy.Streamed[DR, Dec, A]
  ) extends DynamoExecution.Dependent[DR, Dec, StreamFThrowable[?[_, `+_`], A]] {

    def map[B](f: A => B): DynamoExecution.Streamed[DR, Dec, B] = {
      copy(executionStrategy = ExecutionStrategy.Streamed[DR, Dec, B] {
        req => ctx =>
          executionStrategy(req)(ctx).map(f)
      })
    }
    def flatMap[B](f: A => B): DynamoExecution.Streamed[DR, Dec, B] = {
      copy(executionStrategy = ExecutionStrategy.Streamed[DR, Dec, B] {
        req => ctx =>
          executionStrategy(req)(ctx).map(f)
      })
    }
    def void: DynamoExecution.Streamed[DR, Dec, Unit] = {
      map(_ => ())
    }
    def through[B](f: StreamFThrowable[UnknownF, A] => StreamFThrowable[UnknownF, B]): DynamoExecution.Streamed[DR, Dec, B] = {
      modifyExecution(stream => _ => f(stream))
    }

    def retryWithPrefix(ddl: TableDDL, sleep: Duration = 1.second)(implicit ev: DR <:< WithTableReference[DR]): DynamoExecution.Streamed[DR, Dec, A] = {
      modifyStrategy(Streamed.retryWithPrefix(ddl, sleep))
    }

    def modify(f: DR => DR): DynamoExecution.Streamed[DR, Dec, A] = {
      copy(dynamoQuery = dynamoQuery.modify(f))
    }
    def modifyQuery(f: DynamoQuery[DR, Dec] => DynamoQuery[DR, Dec]): DynamoExecution.Streamed[DR, Dec, A] = {
      copy(dynamoQuery = f(dynamoQuery))
    }
    def modifyStrategy[B](f: ExecutionStrategy.Streamed[DR, Dec, A] => ExecutionStrategy.Streamed[DR, Dec, B]): DynamoExecution.Streamed[DR, Dec, B] = {
      copy(executionStrategy = f(executionStrategy))
    }
    def modifyExecution[B](
      f: StreamFThrowable[UnknownF, A] => DynamoExecutionContext[UnknownF] => StreamFThrowable[UnknownF, B]
    ): DynamoExecution.Streamed[DR, Dec, B] = {
      copy(executionStrategy = ExecutionStrategy.Streamed[DR, Dec, B] {
        query => ctx =>
          f(executionStrategy(query)(ctx))(ctx)
      })
    }
  }

  object Streamed {

    def streamed[DR <: DynamoRequest, Dec](implicit paging: PageableRequest[DR]): ExecutionStrategy.Streamed[DR, Dec, Dec] =
      ExecutionStrategy.Streamed[DR, Dec, Dec] {
        req => ctx =>
          import ctx._

          Stream
            .eval(for {
              lastEvaluatedKey <- Ref.of(Option.empty[paging.PageMarker])
              stream = Stream.repeatEval {
                ctx.streamExecutionWrapper {
                  for {
                    oldKey <- lastEvaluatedKey.get
                    newReq = req.modify(paging.withPageMarkerOption(_, oldKey))

                    newRsp <- interpreter.run(newReq)
                    newKey = paging.getPageMarker(newRsp)
                    _      <- lastEvaluatedKey.set(newKey)

                    continue = newKey.isDefined
                    decoded  <- req.decoder[F](newRsp)
                  } yield continue -> decoded
                }
              }.takeThrough(_._1).map(_._2)
            } yield stream).flatten
      }

    def streamedFlatten[DR <: DynamoRequest: PageableRequest, Dec: ? <:< List[A], A]: ExecutionStrategy.Streamed[DR, Dec, A] =
      ExecutionStrategy.Streamed[DR, Dec, A] {
        req => ctx =>
          streamed[DR, Dec].apply(req)(ctx).flatMap(Stream.emits(_))
      }

    def retryWithPrefix[DR <: DynamoRequest, Dec, A](ddl: TableDDL, sleep: Duration = 1.second)(nested: ExecutionStrategy.Streamed[DR, Dec, A])(
      implicit ev: DR <:< WithTableReference[DR]
    ): ExecutionStrategy.Streamed[DR, Dec, A] = ExecutionStrategy.Streamed[DR, Dec, A] {
      req => ctx =>
        import ctx._

        val newTableReq = DynamoExecution.createTable(req.table, ddl)
        val mkTable     = newTableReq.executionStrategy(newTableReq.dynamoQuery)(ctx)

        retryIfTableNotFound[Stream[F[Throwable, ?], ?], A](attempts = 120, Stream.eval(F.sleep(sleep)))(Stream.eval(mkTable)) {
          nested(req)(ctx)
        }
    }

  }

  trait Dependent[DR <: DynamoRequest, Dec, +Out0[_[_, _]]] {
    def dynamoQuery: DynamoQuery[DR, Dec]
    def executionStrategy: ExecutionStrategy.Dependent[DR, Dec, Out0]
  }

}
