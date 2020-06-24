package d4s.models

import cats.MonadError
import cats.effect.concurrent.Ref
import d4s.models.ExecutionStrategy.{FThrowable, StrategyInput, StreamFThrowable, UnknownF}
import d4s.models.query.DynamoRequest.{PageableRequest, WithLimit, WithProjectionExpression, WithSelect, WithTableReference}
import d4s.models.query.requests._
import d4s.models.query.{DynamoQuery, DynamoRequest}
import d4s.models.table.{TableDDL, TableReference}
import fs2.Stream
import izumi.functional.bio.BIOMonadError
import izumi.functional.bio.catz._
import software.amazon.awssdk.services.dynamodb.model.{ConditionalCheckFailedException, CreateTableResponse, ResourceInUseException, ResourceNotFoundException}

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.language.reflectiveCalls
import scala.reflect.ClassTag

final case class DynamoExecution[DR <: DynamoRequest, Dec, +A](
  dynamoQuery: DynamoQuery[DR, Dec],
  executionStrategy: ExecutionStrategy[DR, Dec, A],
) extends DynamoExecution.Dependent[DR, Dec, FThrowable[?[_, `+_`], A]] {

  def map[B](f: A => B): DynamoExecution[DR, Dec, B] = {
    modifyExecution(io => _.F.map(io)(f))
  }
  def flatMap[B](f: A => StrategyInput[UnknownF, DR, Dec] => UnknownF[Throwable, B]): DynamoExecution[DR, Dec, B] = {
    modifyExecution(io => ctx => ctx.F.flatMap(io)(f(_)(ctx)))
  }
  def void: DynamoExecution[DR, Dec, Unit] = {
    map(_ => ())
  }

  def redeem[B](
    err: Throwable => StrategyInput[UnknownF, DR, Dec] => UnknownF[Throwable, B],
    succ: A => StrategyInput[UnknownF, DR, Dec] => UnknownF[Throwable, B],
  ): DynamoExecution[DR, Dec, B] = {
    modifyExecution(io => ctx => ctx.F.redeem(io)(err(_)(ctx), succ(_)(ctx)))
  }
  def catchAll[A1 >: A](err: Throwable => StrategyInput[UnknownF, DR, Dec] => UnknownF[Throwable, A1]): DynamoExecution[DR, Dec, A1] = {
    redeem(err, a => _.F.pure(a))
  }

  def eitherConditionSuccess: DynamoExecution[DR, Dec, Either[ConditionalCheckFailedException, A]] = {
    redeem(
      {
        case DynamoException(_, err: ConditionalCheckFailedException) => _.F.pure(Left(err))
        case err: Throwable                                           => _.F.fail(err)
      },
      a => _.F.pure(Right(a)),
    ).discardInterpreterError[ConditionalCheckFailedException]
  }
  def optConditionFailure: DynamoExecution[DR, Dec, Option[ConditionalCheckFailedException]] = {
    eitherConditionSuccess.map(_.left.toOption)
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
  def modifyExecution[B](f: UnknownF[Throwable, A] => StrategyInput[UnknownF, DR, Dec] => UnknownF[Throwable, B]): DynamoExecution[DR, Dec, B] = {
    copy(executionStrategy = ExecutionStrategy[DR, Dec, B] {
      in =>
        f(executionStrategy(in))(in)
    })
  }

  /** Methods below will mutate interpreter error handler.
    * You can use them to log any error raised during request execution on interpreter level.
    */
  def discardInterpreterError[Err: ClassTag]: DynamoExecution[DR, Dec, A] = {
    tapInterpreterError(in => { case DynamoException(_, _: Err) => in.F.unit })
  }
  def tapInterpreterError(
    handler: StrategyInput[UnknownF, DR, Dec] => PartialFunction[DynamoException, UnknownF[Nothing, Unit]]
  ): DynamoExecution[DR, Dec, A] = {
    copy(executionStrategy = ExecutionStrategy[DR, Dec, A] {
      in =>
        executionStrategy(in.tapInterpreterError(handler(in)))
    })
  }
}

object DynamoExecution {

  implicit final class RetryWithPrefix[DR <: DynamoRequest with WithTableReference[DR], Dec, +A](
    private val exec: DynamoExecution[DR, Dec, A]
  ) extends AnyVal {
    def retryWithPrefix(ddl: TableDDL, sleep: Duration = 1.second): DynamoExecution[DR, Dec, A] = {
      exec.modifyStrategy(DynamoExecution.retryWithPrefix(ddl, sleep))
    }
  }

  def apply[DR <: DynamoRequest, Dec](dynamoQuery: DynamoQuery[DR, Dec]): DynamoExecution[DR, Dec, Dec] = {
    new DynamoExecution[DR, Dec, Dec](dynamoQuery, DynamoExecution.single[DR, Dec])
  }

  def single[DR <: DynamoRequest, Dec]: ExecutionStrategy[DR, Dec, Dec] = {
    ExecutionStrategy {
      in =>
        import in._
        in.interpreter
          .run(in.query, in.interpreterErrorLogger)
          .flatMap(in.query.decoder(_))
    }
  }

  // @formatter:off
  def createTable[F[+_, +_]](table: TableReference, ddl: TableDDL, sleep: Duration = 1.second): DynamoExecution[CreateTable, CreateTableResponse, Unit] = {
    CreateTable(table, ddl).toQuery.exec
      .discardInterpreterError[ResourceInUseException].redeem(
        {
          case DynamoException(_, _: ResourceInUseException) => _.F.unit
          case e                                             => _.F.fail(e)
        }, {
          rsp => in =>
            import in._

            val resourceNotFoundHandler: PartialFunction[Throwable, UnknownF[Nothing, Unit]] = { case _: ResourceNotFoundException => F.unit }
            val updateTTL = table.ttlField match {
              case None    => F.unit
              case Some(_) => interpreter.run(DynamoQuery(UpdateTTL(table)), resourceNotFoundHandler).void
            }
            val tagResources = F.when(table.tags.nonEmpty) {
              interpreter.run(DynamoQuery(UpdateTableTags(table, rsp.tableDescription().tableArn())), resourceNotFoundHandler).void
            }
            val updateContinuousBackups = ddl.backupEnabled match {
              case Some(true) => interpreter.run(DynamoQuery(UpdateContinuousBackups(table, backupEnabled = true)), resourceNotFoundHandler)
              case _          => F.unit
            }

            // wait until the table appears
            retryIfTableNotFound(attempts = 120, F.sleep(sleep).widenError[Throwable])(F.unit) {
              updateTTL *> tagResources *> updateContinuousBackups.void
            }
        }
      )
  }
  // @formatter:on

  def listTables: DynamoExecution[ListTables, List[String], List[String]] = {
    ListTables().toQuery.decode(_.tableNames().asScala.toList).execPagedFlatten()
  }

  def listTablesStream: DynamoExecution.Streamed[ListTables, List[String], String] = {
    ListTables().toQuery.decode(_.tableNames().asScala.toList).execStreamedFlatten
  }

  def offset[DR <: DynamoRequest with WithSelect[DR] with WithLimit[DR] with WithProjectionExpression[DR], Dec, A](
    offsetLimit: OffsetLimit
  )(implicit
    paging: PageableRequest[DR],
    ev2: DR#Rsp => { def count(): Integer },
    ev3: Dec <:< List[A],
  ): ExecutionStrategy[DR, Dec, List[A]] = ExecutionStrategy {
    in =>
      import in._
      import paging.PageMarker

      def firstOffsetKey(): F[Throwable, Option[PageMarker]] = {
        def go(lastEvaluatedKey: Option[PageMarker], fetched: Int): F[Throwable, Option[PageMarker]] = {
          lastEvaluatedKey match {
            case None                               => F.pure(lastEvaluatedKey)
            case _ if fetched == offsetLimit.offset => F.pure(lastEvaluatedKey)
            case Some(nextPage) =>
              val newRq = query.modify(paging.withPageMarker(_, nextPage).withLimit(offsetLimit.offset - fetched))
              interpreter
                .run(newRq, in.interpreterErrorLogger)
                .flatMap(newRsp => go(paging.getPageMarker(newRsp), fetched + newRsp.count()))
          }
        }

        for {
          skipOffsetRsp <- interpreter.run(query.withLimit(offsetLimit.offset).countOnly, in.interpreterErrorLogger)
          res           <- go(paging.getPageMarker(skipOffsetRsp), skipOffsetRsp.count())
        } yield res
      }

      if (offsetLimit.offset <= 0) {
        pagedFlatten[DR, Dec, A](Some(offsetLimit.limit.toInt)).apply(StrategyInput(query, F, interpreter, interpreterErrorLogger = interpreterErrorLogger))
      } else {
        for {
          lastKey <- firstOffsetKey()
          newReq = query.modify {
            rq =>
              lastKey
                .fold(rq)(paging.withPageMarker(rq, _))
                .withLimit(offsetLimit.limit.toInt)
          }
          res <- pagedFlatten[DR, Dec, A](Some(offsetLimit.limit.toInt)).apply(StrategyInput(newReq, F, interpreter, interpreterErrorLogger = interpreterErrorLogger))
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
  )(f: List[Dec] => List[A]
  )(implicit paging: PageableRequest[DR]
  ): ExecutionStrategy[DR, Dec, List[A]] =
    ExecutionStrategy {
      in =>
        import in._

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
              val newReq = query.modify(paging.withPageMarker(_, nextPage))
              (for {
                newRsp  <- interpreter.run(newReq, interpreterErrorLogger)
                decoded <- query.decoder[F](newRsp)
              } yield go(newRsp, rows :+ decoded)).flatMap(identity)
          }
        }

        for {
          newRsp  <- interpreter.run(query, interpreterErrorLogger)
          decoded <- query.decoder[F](newRsp)
          res     <- go(newRsp, Queue(decoded))
        } yield res
    }

  def retryWithPrefix[DR <: DynamoRequest with WithTableReference[DR], Dec, A](
    ddl: TableDDL,
    sleep: Duration = 1.second,
  )(nested: ExecutionStrategy[DR, Dec, A]
  ): ExecutionStrategy[DR, Dec, A] = ExecutionStrategy[DR, Dec, A] {
    in =>
      import in._

      val newTableReq = DynamoExecution.createTable(query.table, ddl)
      val mkTable     = newTableReq.executionStrategy(StrategyInput(newTableReq.dynamoQuery, F, interpreter))

      retryIfTableNotFound[F[Throwable, ?], A](attempts = 120, F.sleep(sleep))(mkTable) {
        nested(in.discardInterpreterError[ResourceNotFoundException])
      }
  }

  private[this] def retryIfTableNotFound[F[_], A](
    attempts: Int,
    sleep: F[Unit],
  )(prepareTable: F[Unit]
  )(attemptAction: F[A]
  )(implicit F: MonadError[F, Throwable]
  ): F[A] = {
    import cats.syntax.applicativeError._
    import cats.syntax.flatMap._

    attemptAction.handleErrorWith {
      case e @ DynamoException(_, _: ResourceNotFoundException | _: ResourceInUseException) =>
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
    executionStrategy: ExecutionStrategy.Streamed[DR, Dec, A],
  ) extends DynamoExecution.Dependent[DR, Dec, StreamFThrowable[?[_, `+_`], A]] {

    def map[B](f: A => B): DynamoExecution.Streamed[DR, Dec, B] = {
      through(_.map(f))
    }
    def flatMap[B](f: A => StreamFThrowable[UnknownF, B]): DynamoExecution.Streamed[DR, Dec, B] = {
      through(_.flatMap(f))
    }
    def void: DynamoExecution.Streamed[DR, Dec, Unit] = {
      map(_ => ())
    }
    def through[B](f: StreamFThrowable[UnknownF, A] => StreamFThrowable[UnknownF, B]): DynamoExecution.Streamed[DR, Dec, B] = {
      modifyExecution(stream => _ => f(stream))
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
      f: StreamFThrowable[UnknownF, A] => StrategyInput[UnknownF, DR, Dec] => StreamFThrowable[UnknownF, B]
    ): DynamoExecution.Streamed[DR, Dec, B] = {
      copy(executionStrategy = ExecutionStrategy.Streamed[DR, Dec, B] {
        in =>
          f(executionStrategy(in))(in)
      })
    }
  }

  object Streamed {

    implicit final class RetryWithPrefix[DR <: DynamoRequest with WithTableReference[DR], Dec, +A](
      private val exec: DynamoExecution.Streamed[DR, Dec, A]
    ) extends AnyVal {
      def retryWithPrefix(ddl: TableDDL, sleep: Duration = 1.second): DynamoExecution.Streamed[DR, Dec, A] = {
        exec.modifyStrategy(Streamed.retryWithPrefix(ddl, sleep))
      }
    }

    def streamed[DR <: DynamoRequest, Dec](implicit paging: PageableRequest[DR]): ExecutionStrategy.Streamed[DR, Dec, Dec] =
      ExecutionStrategy.Streamed[DR, Dec, Dec] {
        in =>
          import in._
          Stream
            .eval(for {
              lastEvaluatedKey <- Ref.of(Option.empty[paging.PageMarker])
              stream = Stream.repeatEval {
                streamExecutionWrapper {
                  for {
                    oldKey <- lastEvaluatedKey.get
                    newReq  = query.modify(paging.withPageMarkerOption(_, oldKey))

                    newRsp <- interpreter.run(newReq, in.interpreterErrorLogger)
                    newKey  = paging.getPageMarker(newRsp)
                    _      <- lastEvaluatedKey.set(newKey)

                    continue = newKey.isDefined
                    decoded <- query.decoder[F](newRsp)
                  } yield continue -> decoded
                }
              }.takeThrough(_._1).map(_._2)
            } yield stream).flatten
      }

    def streamedFlatten[DR <: DynamoRequest: PageableRequest, Dec: ? <:< List[A], A]: ExecutionStrategy.Streamed[DR, Dec, A] =
      ExecutionStrategy.Streamed[DR, Dec, A] {
        in =>
          streamed[DR, Dec].apply(in).flatMap(Stream.emits(_))
      }

    def retryWithPrefix[DR <: DynamoRequest with WithTableReference[DR], Dec, A](
      ddl: TableDDL,
      sleep: Duration = 1.second,
    )(nested: ExecutionStrategy.Streamed[DR, Dec, A]
    ): ExecutionStrategy.Streamed[DR, Dec, A] = ExecutionStrategy.Streamed[DR, Dec, A] {
      in =>
        import in._

        val newTableReq = DynamoExecution.createTable(query.table, ddl)
        val mkTable     = newTableReq.executionStrategy(StrategyInput(newTableReq.dynamoQuery, F, interpreter))

        retryIfTableNotFound[Stream[F[Throwable, ?], ?], A](attempts = 120, Stream.eval(F.sleep(sleep)))(Stream.eval(mkTable)) {
          nested(in.discardInterpreterError[ResourceNotFoundException])
        }
    }

  }

  trait Dependent[DR <: DynamoRequest, Dec, +Out0[_[_, _]]] {
    def dynamoQuery: DynamoQuery[DR, Dec]
    def executionStrategy: ExecutionStrategy.Dependent[DR, Dec, Out0]
  }

}
