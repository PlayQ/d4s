package d4s

import d4s.config.{DynamoBatchConfig, DynamoConfig}
import d4s.models.DynamoException
import d4s.models.DynamoException.InterpreterException
import d4s.models.ExecutionStrategy.StrategyInput
import d4s.models.query.DynamoRequest.{DynamoWriteBatchRequest, PageableRequest, WithBatch, WithProjectionExpression, WithTableReference}
import d4s.models.query._
import d4s.models.query.requests._
import d4s.models.query.responses.HasItems
import izumi.functional.bio.catz._
import izumi.functional.bio.{Async2, Error2, F, Fork2, Temporal2}
import logstage.LogIO2
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model._

import scala.concurrent.duration._

trait DynamoInterpreter[F[_, _]] {
  def run[DR <: DynamoRequest, Dec](
    q: DynamoQuery[DR, Dec],
    tapError: PartialFunction[DynamoException, F[Nothing, Unit]],
  ): F[DynamoException, DR#Rsp]
}

object DynamoInterpreter {
  final class Impl[F[+_, +_]: Async2: Temporal2: Fork2](
    client: DynamoClient[F],
    batchConfig: DynamoBatchConfig,
    dynamoConfig: DynamoConfig,
  )(implicit
    log: LogIO2[F]
  ) extends DynamoInterpreter[F] {

    override def run[DR <: DynamoRequest.Aux[_, _], Dec](
      q: DynamoQuery[DR, Dec],
      tapError: PartialFunction[DynamoException, F[Nothing, Unit]],
    ): F[DynamoException, DR#Rsp] = {
      runImpl[DR, Dec, DR#Rq, DR#Rsp](q, tapError)
    }

    private[this] def runImpl[DR <: DynamoRequest.Aux[Rq, Rsp], Dec, Rq, Rsp](
      query: DynamoQuery[DR, Dec],
      tapError: PartialFunction[DynamoException, F[Nothing, Unit]],
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
        case q: CreateBackup =>
          createBackup(q.toAmz).logWrapError("CreateBackup", q.table.fullName, tapError) <*
          log.info(s"Created backup of ${q.table.fullName -> "tableName"}.")
        case q: UpdateTableTags =>
          updateTableTags(q.toAmz).logWrapError("UpdateTableTags", q.table.fullName, tapError) <*
          log.info(s"Tags for ${q.table.fullName -> "tableName"} were updated, ${q.dynamoResourceName}. ${q.table.tags ++ q.tagsToAdd -> "tags"}")
        case q: UpdateTable =>
          client.raw(_.updateTable(q.toAmz)).logWrapError("UpdateTable", q.table.fullName, tapError) <*
          log.info(s"Table ${q.table.fullName -> "tableName"} were updated, ${q.toAmz.toString -> "request"}")
        case q: UpdateContinuousBackups =>
          updateContinuousBackups(q.toAmz).logWrapError("UpdateContinuousBackups", q.table.fullName, tapError) <*
          log.info(s"Continuous backup for ${q.table.fullName -> "tableName"} were updated, ${q.backupEnabled}.")

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
        case q: QueryDeleteBatch => runStreamDeleteBatch(q.wrapped.toQuery, q.maxParallelDeletes, tapError).logWrapError("QueryDeleteBatch", q.table.fullName, tapError)
        case q: ScanDeleteBatch  => runStreamDeleteBatch(q.wrapped.toQuery, q.maxParallelDeletes, tapError).logWrapError("ScanDeleteBatch", q.table.fullName, tapError)

        case r: RawRequest[_, _] => r.interpret(r.toAmz)(F, client).logWrapError("RawRequest")

        case other => F.fail(DynamoException.InterpreterException("UNKNOWN", None, new RuntimeException(s"Unexpected internal in interpreter: $other.")))
      }
    }

    private[this] def runStreamDeleteBatch[DR <: DynamoRequest with WithProjectionExpression[DR] with WithTableReference[DR]](
      dynamoQuery: DynamoQuery[DR, _],
      parallelism: Option[Int],
      interpreterErrorLogger: PartialFunction[DynamoException, F[Nothing, Unit]],
    )(implicit ev: HasItems[DR#Rsp],
      ev1: PageableRequest[DR],
    ): F[Throwable, List[BatchWriteItemResponse]] = {
      import scala.jdk.CollectionConverters._

      val exec = dynamoQuery
        .withProjectionExpression(dynamoQuery.table.key.keyFields.toList: _*)
        .decode(ev.items(_).asScala.map(_.asScala.toMap).toList)
        .execStreamedFlatten

      exec
        .executionStrategy(StrategyInput(exec.dynamoQuery, this, interpreterErrorLogger = interpreterErrorLogger))
        .chunkN(batchConfig.writeBatchSize)
        .parEvalMap(parallelism.getOrElse(Int.MaxValue))(itemsChunk => runWriteBatch(DeleteItemBatch(dynamoQuery.table, itemsChunk.toList)))
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
            _.batchWriteItem(_)
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
    )(raw: (DynamoDbClient, Rq) => Rsp
    )(getUnprocessed: Rsp => java.util.Map[K, V]
    )(mkNewRq: (Rq, java.util.Map[K, V]) => Rq
    ): F[Throwable, Rsp] = {
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

    private[this] def createBackup(request: CreateBackupRequest): F[Throwable, CreateBackupResponse] = {
      if (dynamoConfig.maybeLocalUrl.isEmpty)
        client.raw(_.createBackup(request))
      else
        log.info(s"Skipping backup creation for mock environment: found ${dynamoConfig.maybeLocalUrl} in $dynamoConfig") *>
        F.pure(CreateBackupResponse.builder().build())
    }

  }

  private[this] implicit final class ThrowableDynamoOps[F[+_, +_], A](private val f: F[Throwable, A]) extends AnyVal {
    def logWrapError(
      operation: String,
      tableName: String,
      errorLogger: PartialFunction[DynamoException, F[Nothing, Unit]],
    )(implicit F: Error2[F],
      log: LogIO2[F],
    ): F[DynamoException, A] = {
      f.leftMap(InterpreterException(operation, Some(tableName), _)).tapError {
        errorLogger.orElse {
          case failure => log.error(s"Dynamo: Got error during executing $operation for $tableName. ${failure.cause -> "Failure"}.")
        }
      }
    }

    def logWrapError(operation: String)(implicit F: Error2[F], log: LogIO2[F]): F[DynamoException, A] = {
      f.tapError(failure => log.error(s"Dynamo: Got error during executing $operation. ${failure.getCause -> "Failure"}"))
        .leftMap(InterpreterException(operation, None, _))
    }
  }

}
