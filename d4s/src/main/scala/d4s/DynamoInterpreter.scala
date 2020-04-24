package d4s

import d4s.config.{DynamoBatchConfig, DynamoConfig}
import d4s.models.DynamoException
import d4s.models.DynamoException.InterpreterException
import d4s.models.ExecutionStrategy.StrategyInput
import d4s.models.query.DynamoRequest.{DynamoWriteBatchRequest, WithBatch}
import d4s.models.query._
import d4s.models.query.requests._
import izumi.functional.bio.catz._
import izumi.functional.bio.{BIOError, BIOFork, BIOTemporal, F}
import logstage.LogBIO
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model._

import scala.concurrent.duration._

trait DynamoInterpreter[F[_, _]] {
  def run[DR <: DynamoRequest, Dec](
    q: DynamoQuery[DR, Dec],
    tapError: PartialFunction[DynamoException, F[Nothing, Unit]]
  ): F[DynamoException, DR#Rsp]
}

object DynamoInterpreter {
  final class Impl[F[+_, +_]: BIOTemporal: BIOFork](
    client: DynamoClient[F],
    batchConfig: DynamoBatchConfig,
    dynamoConfig: DynamoConfig,
  )(implicit
    log: LogBIO[F],
  ) extends DynamoInterpreter[F] {

    override def run[DR <: DynamoRequest.Aux[_, _], Dec](
      q: DynamoQuery[DR, Dec],
      tapError: PartialFunction[DynamoException, F[Nothing, Unit]]
    ): F[DynamoException, DR#Rsp] = {
      runImpl[DR, Dec, DR#Rq, DR#Rsp](q, tapError)
    }

    private[this] def runImpl[DR <: DynamoRequest.Aux[Rq, Rsp], Dec, Rq, Rsp](
      query: DynamoQuery[DR, Dec],
      tapError: PartialFunction[DynamoException, F[Nothing, Unit]]
    ): F[DynamoException, Rsp] = {
      query.request match {
        case q: CreateTable =>
          client.raw(_.createTable(q.toAmz)).logWrapError("CreateTable", q.table.fullName, tapError) <*
          log.info(s"Created ${q.table.fullName -> "tableName"}; ${q.ddl.provisioning -> "provisioning"}.")
        case q: UpdateTTL =>
          client.raw(_.updateTimeToLive(q.toAmz)).logWrapError("UpdateTimeToLive", q.table.fullName, tapError) <*
          log.info(s"Updated TTL for ${q.table.fullName -> "tableName"} ${q.table.ttlField -> "ttlField"}.")
        case q: DeleteTable =>
          client.raw(_.deleteTable(q.toAmz)).logWrapError("DeleteTable", q.table.fullName, tapError) <*
          log.info(s"Deleted ${q.table.fullName -> "tableName"}.")
        case q: UpdateTableTags =>
          updateTableTags(q.toAmz).logWrapError("UpdateTableTags", q.table.fullName, tapError) <*
          log.info(s"Tags for ${q.table.fullName -> "tableName"} were updated, ${q.dynamoResourceName}. ${q.table.tags ++ q.tagsToAdd -> "tags"}")
        case q: UpdateTable =>
          client.raw(_.updateTable(q.toAmz)).logWrapError("UpdateTable", q.table.fullName, tapError) <*
          log.info(s"Table ${q.table.fullName -> "tableName"} were updated, ${q.toAmz.toString -> "request"}")

        case q: DescribeTable => client.raw(_.describeTable(q.toAmz)).logWrapError("DescribeTable", q.table.fullName, tapError)
        case q: ListTables    => client.raw(_.listTables(q.toAmz)).logWrapError("ListTables")

        case q: Query => client.raw(_.query(q.toAmz)).logWrapError("Query", q.table.fullName, tapError)
        case q: Scan  => client.raw(_.scan(q.toAmz)).logWrapError("Scan", q.table.fullName, tapError)

        case q: GetItem    => client.raw(_.getItem(q.toAmz)).logWrapError("GetItem", q.table.fullName, tapError)
        case q: PutItem    => client.raw(_.putItem(q.toAmz)).logWrapError("PutItem", q.table.fullName, tapError)
        case q: DeleteItem => client.raw(_.deleteItem(q.toAmz)).logWrapError("DeleteItem", q.table.fullName, tapError)
        case q: UpdateItem => client.raw(_.updateItem(q.toAmz)).logWrapError("UpdateItem", q.table.fullName, tapError)

        case q: DeleteItemBatch  => runWriteBatch(q).logWrapError("DeleteItemBatch", q.table.fullName, tapError)
        case q: PutItemBatch     => runWriteBatch(q).logWrapError("WriteItemBatch", q.table.fullName, tapError)
        case q: GetItemBatch     => runGetBatch(q).logWrapError("GetItemBatch", q.table.fullName, tapError)
        case q: QueryDeleteBatch => runQueryDeleteBatch(q).logWrapError("QueryDeleteBatch", q.table.fullName, tapError)

        case q: UpdateContinuousBackups => updateContinuousBackups(q.toAmz).logWrapError("UpdateContinuousBackups", q.table.fullName, tapError)

        case r: RawRequest[_, _] => r.interpret(r.toAmz)(F, client).logWrapError("RawRequest")
      }
    }

    private[this] def runQueryDeleteBatch(rq: QueryDeleteBatch): F[Throwable, List[BatchWriteItemResponse]] = {
      import scala.jdk.CollectionConverters._

      val exec = rq.toRegularQuery.toQuery
        .withProjectionExpression(rq.table.key.keyFields.toList: _*)
        .decode(_.items().asScala.map(_.asScala.toMap).toList)
        .execStreamedFlatten

      exec
        .executionStrategy(StrategyInput(exec.dynamoQuery, F, this))
        .chunkN(batchConfig.writeBatchSize)
        .parEvalMap(rq.maxParallelDeletes.getOrElse(Int.MaxValue))(itemsChunk => runWriteBatch(DeleteItemBatch(rq.table, itemsChunk.toList)))
        .flatMap(fs2.Stream.emits)
        .compile.toList
    }

    private[this] def runWriteBatch[DR <: DynamoWriteBatchRequest, BT[_]](
      request: DR with WithBatch[DR, BT]
    ): F[Throwable, List[BatchWriteItemResponse]] = {
      val batches = request.batchItems.grouped(batchConfig.writeBatchSize).toList

      F.traverse(batches) {
        items =>
          batchLoop(request.withBatch(items).toAmz)(
            _.batchWriteItem(_),
          )(_.unprocessedItems())(
            _.toBuilder.requestItems(_).build()
          )
      }
    }

    private[this] def runGetBatch(rq: GetItemBatch): F[Throwable, List[BatchGetItemResponse]] = {
      val batches = rq.batchItems.grouped(batchConfig.getBatchSize).toList
      F.traverse(batches) {
        items =>
          batchLoop(rq.withBatch(items).toAmz)(
            _.batchGetItem(_)
          )(_.unprocessedKeys())(
            _.toBuilder.requestItems(_).build()
          )
      }
    }

    @inline private[this] def batchLoop[Rq, Rsp, K, V](
      rq0: Rq
    )(raw: (DynamoDbClient, Rq) => Rsp)(
      getUnprocessed: Rsp => java.util.Map[K, V]
    )(mkNewRq: (Rq, java.util.Map[K, V]) => Rq): F[Throwable, Rsp] = {
      val sleepDuration = batchConfig.unprocessedBatchSleep getOrElse 1.second

      def go(rq: Rq): F[Throwable, Rsp] = {
        client.raw(raw(_, rq)).flatMap {
          rsp =>
            val unprocessed = getUnprocessed(rsp)

            if (unprocessed.isEmpty) {
              F.pure(rsp)
            } else {
              val newRq = mkNewRq(rq, unprocessed)

              F.sleep(sleepDuration) *>
              go(newRq)
            }
        }
      }

      go(rq0)
    }

    private[this] def updateTableTags(request: TagResourceRequest): F[Throwable, TagResourceResponse] = {
      if (dynamoConfig.maybeLocalUrl.isEmpty)
        client.raw(_.tagResource(request))
      else
        log.info(s"Skipping update tags request for mock environment: found ${dynamoConfig.maybeLocalUrl} in $dynamoConfig") *>
        F.pure(TagResourceResponse.builder().build())
    }

    private[this] def updateContinuousBackups(request: UpdateContinuousBackupsRequest): F[Throwable, UpdateContinuousBackupsResponse] = {
      if (dynamoConfig.maybeLocalUrl.isEmpty)
        client.raw(_.updateContinuousBackups(request))
      else
        log.info(s"Skipping update continuous backups for mock environment: found ${dynamoConfig.maybeLocalUrl} in $dynamoConfig") *>
        F.pure(UpdateContinuousBackupsResponse.builder().build())
    }

  }

  private[this] implicit final class ThrowableDynamoOps[F[+_, +_], A](private val f: F[Throwable, A]) extends AnyVal {
    def logWrapError(
      operation: String,
      tableName: String,
      errorHandler: PartialFunction[DynamoException, F[Nothing, Unit]]
    )(implicit F: BIOError[F], log: LogBIO[F]): F[DynamoException, A] = {
      f.leftMap(InterpreterException(operation, Some(tableName), _)).tapError {
        errorHandler orElse {
          case failure => log.error(s"Dynamo: Got error during executing $operation for $tableName. ${failure.cause -> "Failure"}.")
        }
      }
    }

    def logWrapError(operation: String)(implicit F: BIOError[F], log: LogBIO[F]): F[DynamoException, A] = {
      f.leftMap(InterpreterException(operation, None, _)) tapError (
        failure => log.error(s"Dynamo: Got error during executing $operation. ${failure.cause -> "Failure"}")
      )
    }
  }

}
