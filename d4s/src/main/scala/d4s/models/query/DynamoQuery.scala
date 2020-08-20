package d4s.models.query

import java.time.ZonedDateTime

import d4s.codecs.{AttributeNames, D4SDecoder, D4SEncoder}
import d4s.config.ProvisionedThroughputConfig
import d4s.implicits._
import d4s.models.DynamoException.DecoderException
import d4s.models.conditions.Condition
import d4s.models.conditions.Condition.{attribute_exists, attribute_not_exists}
import d4s.models.query.DynamoRequest._
import d4s.models.query.requests.UpdateTable
import d4s.models.query.responses.{HasAttributes, HasConsumedCapacity, HasItem, HasItems, HasScannedCount}
import d4s.models.table.index.{GlobalIndexUpdate, ProvisionedGlobalIndex, TableIndex}
import d4s.models.table.{DynamoField, TableDDL, TableReference}
import d4s.models.{DynamoExecution, FnBIO, OffsetLimit}
import izumi.functional.bio.{BIO, BIOError, F}
import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, BatchGetItemResponse, ConsumedCapacity, ReturnValue, Select}

import scala.language.implicitConversions

final case class DynamoQuery[DR <: DynamoRequest, +Dec](
  request: DR,
  decoder: FnBIO[DR#Rsp, Dec],
) {
  def toAmz: DR#Rq = request.toAmz

  def modify(f: DR => DR): DynamoQuery[DR, Dec] = copy(request = f(request))

  def decode[C1](f: DR#Rsp => C1): DynamoQuery[DR, C1] = copy(decoder = FnBIO.lift(f))

  def decodeF[C1](f: FnBIO[DR#Rsp, C1]): DynamoQuery[DR, C1] = copy(decoder = f)

  def decodeWith[C1](f: (DR#Rsp, Dec) => C1): DynamoQuery[DR, C1] = decodeWithF(FnBIO.lift(f.tupled))

  def decodeWithF[C1](f: FnBIO[(DR#Rsp, Dec), C1]): DynamoQuery[DR, C1] =
    copy(decoder = new FnBIO[DR#Rsp, C1] {
      override def apply[F[+_, +_]: BIO](b: DR#Rsp): F[Throwable, C1] = {
        decoder[F](b).flatMap(c => f[F]((b, c)))
      }
    })
}

object DynamoQuery {
  def apply[DR <: DynamoRequest](request: DR): DynamoQuery[DR, DR#Rsp] = DynamoQuery[DR, DR#Rsp](request, FnBIO.lift(identity[DR#Rsp]))

  @inline implicit final def toDynamoExecution[DR <: DynamoRequest, Dec](
    dynamoQuery: DynamoQuery[DR, Dec]
  ): DynamoExecution[DR, Dec, Dec] = {
    DynamoExecution(dynamoQuery, DynamoExecution.single[DR, Dec])
  }

  implicit final class Exec[DR <: DynamoRequest, Dec](
    private val dynamoQuery: DynamoQuery[DR, Dec]
  ) extends AnyVal {
    def exec: DynamoExecution[DR, Dec, Dec] = toDynamoExecution(dynamoQuery)
  }

  implicit final class ExecPagedFlatten[DR <: DynamoRequest, A](
    private val dynamoQuery: DynamoQuery[DR, List[A]]
  ) extends AnyVal {
    def execPagedFlatten(limit: Option[Int] = None)(implicit paging: PageableRequest[DR]): DynamoExecution[DR, List[A], List[A]] = {
      DynamoExecution(dynamoQuery, DynamoExecution.pagedFlatten[DR, List[A], A](limit))
    }
  }

  implicit final class ExecPaged[DR <: DynamoRequest, Dec](
    private val dynamoQuery: DynamoQuery[DR, Dec]
  ) extends AnyVal {
    def execPaged(limit: Option[Int] = None)(implicit paging: PageableRequest[DR]): DynamoExecution[DR, Dec, List[Dec]] = {
      DynamoExecution(dynamoQuery, DynamoExecution.paged[DR, Dec](limit))
    }

    def execStreamed(implicit paging: PageableRequest[DR]): DynamoExecution.Streamed[DR, Dec, Dec] = {
      DynamoExecution.Streamed[DR, Dec, Dec](dynamoQuery, DynamoExecution.Streamed.streamed[DR, Dec])
    }
  }

  implicit final class ExecStreamFlatten[DR <: DynamoRequest, A](
    private val dynamoQuery: DynamoQuery[DR, List[A]]
  ) extends AnyVal {
    def execStreamedFlatten(implicit paging: PageableRequest[DR]): DynamoExecution.Streamed[DR, List[A], A] = {
      DynamoExecution.Streamed[DR, List[A], A](dynamoQuery, DynamoExecution.Streamed.streamedFlatten[DR, List[A], A])
    }
  }

  implicit final class ExecOffset[DR <: DynamoRequest with WithSelect[DR] with WithLimit[DR] with WithProjectionExpression[DR], A](
    private val dynamoQuery: DynamoQuery[DR, List[A]]
  ) extends AnyVal {
    def execOffset(offsetLimit: OffsetLimit)(implicit paging: PageableRequest[DR], ev: HasScannedCount[DR#Rsp]): DynamoExecution[DR, List[A], List[A]] = {
      new DynamoExecution[DR, List[A], List[A]](dynamoQuery, DynamoExecution.offset[DR, List[A], A](offsetLimit))
    }
  }

  implicit final class RetryWithPrefix[DR <: DynamoRequest with WithTableReference[DR], Dec](
    private val query: DynamoQuery[DR, Dec]
  ) extends AnyVal {
    import scala.concurrent.duration._
    def retryWithPrefix(ddl: TableDDL, sleep: Duration = 1.second): DynamoExecution[DR, Dec, Dec] = {
      query.modifyStrategy(DynamoExecution.retryWithPrefix(ddl, sleep))
    }
  }

  implicit final class TweakFilterExpression[DR <: DynamoRequest with WithFilterExpression[DR], Dec](
    dynamoQuery: DynamoQuery[DR, Dec]
  ) extends WithFilterExpression[DynamoQuery[DR, Dec]] {
    @inline def withFilterExpression(c: Condition): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withFilterExpression(c))
    }
  }

  implicit final class TweakAttributeValues[DR <: DynamoRequest with WithAttributeValues[DR], Dec](
    dynamoQuery: DynamoQuery[DR, Dec]
  ) extends WithAttributeValues[DynamoQuery[DR, Dec]] {
    @inline def withAttributeValues(f: Map[String, AttributeValue] => Map[String, AttributeValue]): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withAttributeValues(f))
    }
  }

  implicit final class TweakAttributeNames[DR <: DynamoRequest with WithAttributeNames[DR], Dec](
    dynamoQuery: DynamoQuery[DR, Dec]
  ) extends WithAttributeNames[DynamoQuery[DR, Dec]] {
    @inline def withAttributeNames(f: Map[String, String] => Map[String, String]): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withAttributeNames(f))
    }
  }

  implicit final class TweakProjectionExpression[DR <: DynamoRequest with WithProjectionExpression[DR], Dec](
    dynamoQuery: DynamoQuery[DR, Dec]
  ) extends WithProjectionExpression[DynamoQuery[DR, Dec]] {
    @inline def withProjectionExpression(f: Option[String] => Option[String]): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withProjectionExpression(f))
    }
  }

  implicit final class TweakTableReference[DR <: DynamoRequest with WithTableReference[DR], Dec](
    dynamoQuery: DynamoQuery[DR, Dec]
  ) extends WithTableReference[DynamoQuery[DR, Dec]] {
    @inline def withTableReference(f: TableReference => TableReference): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withTableReference(f))
    }
    @inline def table: TableReference = dynamoQuery.request.table
  }

  implicit final class TweakIndex[DR <: DynamoRequest with WithIndex[DR], Dec](
    dynamoQuery: DynamoQuery[DR, Dec]
  ) extends WithIndex[DynamoQuery[DR, Dec]] {
    @inline override def withIndex(index: TableIndex[_, _]): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withIndex(index))
    }
  }

  implicit final class TweakLimit[DR <: DynamoRequest with WithLimit[DR], Dec](
    dynamoQuery: DynamoQuery[DR, Dec]
  ) extends WithLimit[DynamoQuery[DR, Dec]] {
    @inline override def withLimit(limit: Int): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withLimit(limit))
    }
  }

  implicit final class TweakStartKey[DR <: DynamoRequest with WithStartKey[DR], Dec](
    dynamoQuery: DynamoQuery[DR, Dec]
  ) extends WithStartKey[DynamoQuery[DR, Dec]] {
    @inline override def withStartKeyMap(startKey: java.util.Map[String, AttributeValue]): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withStartKeyMap(startKey))
    }
  }

  implicit final class TweakBatchItems[DR <: DynamoRequest with WithBatch[DR, BatchType], BatchType[_], Dec](
    dynamoQuery: DynamoQuery[DR, Dec] with DynamoQuery[_ <: DynamoRequest with WithBatch[DR, BatchType], Dec] // satisfy intellij & scalac gods
  ) extends WithBatch[DynamoQuery[DR, Dec], BatchType] {
    @inline override def withBatch[I: D4SEncoder](batchItems: List[BatchType[I]]): DynamoQuery[DR, Dec] = {
      (dynamoQuery: DynamoQuery[DR, Dec]).modify(_.withBatch(batchItems))
    }
    @inline override def withBatch(batchItems: List[Map[String, AttributeValue]]): DynamoQuery[DR, Dec] = {
      (dynamoQuery: DynamoQuery[DR, Dec]).modify(_.withBatch(batchItems))
    }
    @inline override def batchItems: List[Map[String, AttributeValue]] = dynamoQuery.request.batchItems
  }

  implicit final class TweakScanIndexForward[DR <: DynamoRequest with WithScanIndexForward[DR], Dec](
    dynamoQuery: DynamoQuery[DR, Dec]
  ) extends WithScanIndexForward[DynamoQuery[DR, Dec]] {
    @inline override def withScanIndexForward(sif: Boolean): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withScanIndexForward(sif))
    }
  }

  implicit final class TweakKey[DR <: DynamoRequest with WithKey[DR], Dec](
    dynamoQuery: DynamoQuery[DR, Dec]
  ) extends WithKey[DynamoQuery[DR, Dec]] {
    @inline override def withKey(f: Map[String, AttributeValue] => Map[String, AttributeValue]): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withKey(f))
    }
  }

  implicit final class TweakItem[DR <: DynamoRequest with WithItem[DR], Dec](
    dynamoQuery: DynamoQuery[DR, Dec]
  ) extends WithItem[DynamoQuery[DR, Dec]] {
    @inline override def withItemAttributeValues(f: Map[String, AttributeValue] => Map[String, AttributeValue]): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withItemAttributeValues(f))
    }
  }

  @SuppressWarnings(Array("UnsafeTraversableMethods"))
  implicit final class TweakExists[DR <: DynamoRequest with WithCondition[DR] with WithTableReference[DR], Dec](
    private val dynamoQuery: DynamoQuery[DR, Dec]
  ) extends AnyVal {
    def ifExists(): DynamoQuery[DR, Dec] = {
      val table = dynamoQuery.table
      dynamoQuery.withCondition(table.key.keyNames.toList.map(s => attribute_exists(List(s)): Condition).reduceLeft(_ && _))
    }

    def ifNotExists(): DynamoQuery[DR, Dec] = {
      val table = dynamoQuery.table
      dynamoQuery.withCondition(table.key.keyNames.toList.map(s => attribute_not_exists(List(s)): Condition).reduceLeft(_ && _))
    }
  }

  implicit final class TweakCondition[DR <: DynamoRequest with WithCondition[DR], Dec](
    dynamoQuery: DynamoQuery[DR, Dec]
  ) extends WithCondition[DynamoQuery[DR, Dec]] {
    @inline def withCondition(c: Condition): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withCondition(c))
    }
  }

  implicit final class TweakUpdateExpression[DR <: DynamoRequest with WithUpdateExpression[DR], Dec](
    dynamoQuery: DynamoQuery[DR, Dec]
  ) extends WithUpdateExpression[DynamoQuery[DR, Dec]] {
    @inline def withUpdateExpression(f: String => String): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withUpdateExpression(f))
    }
  }

  implicit final class TweakWithTtl[DR <: DynamoRequest with WithAttributeValues[DR] with WithTableReference[DR] with WithFilterExpression[DR], Dec](
    private val dynamoQuery: DynamoQuery[DR, Dec]
  ) extends AnyVal {
    def filterTtl(now: ZonedDateTime): DynamoQuery[DR, Dec] = {
      filterTtl(now.toEpochSecond)
    }

    def filterTtl(nowEpochSeconds: Long): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify {
        rq =>
          rq.table.ttlField.fold(rq) {
            ttlField =>
              rq.withFilterExpression(ttlField.of[Long] >= nowEpochSeconds || ttlField.notExists)
          }
      }
    }
  }

  implicit final class TweakWithTtlField[DR <: DynamoRequest with WithItem[DR] with WithTableReference[DR], Dec](
    private val dynamoQuery: DynamoQuery[DR, Dec]
  ) extends AnyVal {
    def withTtlFieldOption(expiration: Option[ZonedDateTime]): DynamoQuery[DR, Dec] =
      expiration.fold(dynamoQuery)(withTtlField)

    def withTtlFieldOption(expirationEpochSeconds: Option[Long])(implicit dummy: DummyImplicit): DynamoQuery[DR, Dec] = {
      expirationEpochSeconds match {
        case Some(expiration) => withTtlField(expiration)
        case None             => dynamoQuery
      }
    }

    def withTtlField(expiration: ZonedDateTime): DynamoQuery[DR, Dec] =
      withTtlField(expiration.toEpochSecond)

    def withTtlField(expirationEpochSeconds: Long): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify {
        rq =>
          rq.table.ttlField.fold(rq) {
            t =>
              rq.withItemField(DynamoField[Long](t))(expirationEpochSeconds)
          }
      }
    }
  }

  implicit final class TweakWithConsistent[DR <: DynamoRequest with WithConsistent[DR], Dec](
    dynamoQuery: DynamoQuery[DR, Dec]
  ) extends WithConsistent[DynamoQuery[DR, Dec]] {
    override def withConsistent(consistentRead: Boolean): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withConsistent(consistentRead))
    }

    def consistent: DynamoQuery[DR, Dec] = withConsistent(true)
  }

  implicit final class TweakWithParallelism[DR <: DynamoRequest with WithParallelism[DR], Dec](
    dynamoQuery: DynamoQuery[DR, Dec]
  ) extends WithParallelism[DynamoQuery[DR, Dec]] {
    @inline override def maxParallelDeletes: Option[Int] = dynamoQuery.request.maxParallelDeletes

    @inline override def withParallelism(parallelism: Int): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withParallelism(parallelism))
    }
  }

  implicit final class TweakReturnValue[DR <: DynamoRequest with WithReturnValue[DR], Dec](
    dynamoQuery: DynamoQuery[DR, Dec]
  ) extends WithReturnValue[DynamoQuery[DR, Dec]] {
    override def withReturnValue(returnValue: ReturnValue): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withReturnValue(returnValue))
    }
  }

  implicit final class UpdateOps[Dec](
    private val dynamoQuery: DynamoQuery[UpdateTable, Dec]
  ) extends AnyVal {
    @inline def withNewProvisioning(provisioning: ProvisionedThroughputConfig): DynamoQuery[UpdateTable, Dec] = {
      dynamoQuery.modify(_.withNewProvisioning(provisioning))
    }

    @inline def withIndexToCreate(index: ProvisionedGlobalIndex[_, _]): DynamoQuery[UpdateTable, Dec] = {
      dynamoQuery.modify(_.withIndexToCreate(index))
    }

    @inline def withIndexesToUpdate(indexes: Set[GlobalIndexUpdate]): DynamoQuery[UpdateTable, Dec] = {
      dynamoQuery.modify(_.withIndexesToUpdate(indexes))
    }

    @inline def withIndexToDelete(index: String): DynamoQuery[UpdateTable, Dec] = {
      dynamoQuery.modify(_.withIndexToDelete(index))
    }
  }

  implicit final class QueryCountOnly[DR <: DynamoRequest with WithSelect[DR] with WithProjectionExpression[DR], Dec](
    private val dynamoQuery: DynamoQuery[DR, Dec]
  ) extends AnyVal {
    def countOnly(implicit ev: HasScannedCount[DR#Rsp]): DynamoQuery[DR, Int] =
      dynamoQuery.modify(_.withSelect(Select.COUNT).withProjectionExpression(_ => None)).decode(ev.count)

    def scannedCountOnly(implicit ev: HasScannedCount[DR#Rsp]): DynamoQuery[DR, Int] =
      dynamoQuery.modify(_.withSelect(Select.COUNT).withProjectionExpression(_ => None)).decode(ev.scannedCount)
  }

  implicit final class QueryCount[DR <: DynamoRequest, Dec](
    private val dynamoQuery: DynamoQuery[DR, Dec]
  ) extends AnyVal {
    def consumedCapacityOnly(implicit ev: HasConsumedCapacity[DR#Rsp]): DynamoQuery[DR, ConsumedCapacity] =
      dynamoQuery.decode(ev.consumedCapacity)

    def withCount(implicit ev: HasScannedCount[DR#Rsp]): DynamoQuery[DR, (Dec, Int)] =
      dynamoQuery.decodeWith((a, c) => (c, ev.count(a)))

    def withScannedCount(implicit ev: HasScannedCount[DR#Rsp]): DynamoQuery[DR, (Dec, Int)] =
      dynamoQuery.decodeWith((a, c) => (c, ev.scannedCount(a)))

    def withConsumedCapacity(implicit ev: HasConsumedCapacity[DR#Rsp]): DynamoQuery[DR, (Dec, ConsumedCapacity)] =
      dynamoQuery.decodeWith((a, c) => (c, ev.consumedCapacity(a)))
  }

  implicit final class DecodeBatchedItems[DR <: DynamoRequest, Dec, A](
    dynamoQuery: DynamoQuery[DR, Dec]
  )(implicit ev1: DR#Rsp <:< List[BatchGetItemResponse]
  ) {

    def decodeItems[Item: D4SDecoder]: DynamoQuery[DR, List[Item]] = {
      dynamoQuery.decodeF(FnBIO {
        rsp => implicit F =>
          import scala.jdk.CollectionConverters._

          val itemsData = rsp.flatMap(_.responses().asScala.values.flatMap(_.asScala).toList)
          F.traverse(itemsData)(decodeItemImpl(_)).map(_.flatten)
      })
    }
  }

  implicit final class DecodeItems[DR <: DynamoRequest with WithProjectionExpression[DR] with WithTableReference[DR], Dec](
    dynamoQuery: DynamoQuery[DR, Dec]
  )(implicit ev: HasItems[DR#Rsp]
  ) {
    def decodeItems[Item: D4SDecoder: AttributeNames]: DynamoQuery[DR, List[Item]] = {
      dynamoQuery
        .modify(
          _.withProjectionExpression(AttributeNames[Item].projectionExpression)
        ).decodeF(FnBIO {
          response => implicit F =>
            import scala.jdk.CollectionConverters._

            val itemsData = ev.items(response).asScala.toList
            F.traverse(itemsData)(decodeItemImpl(_)).map(_.flatten)
        })
    }

    def decodeItemsWithTTL[Item: D4SDecoder: AttributeNames]: DynamoQuery[DR, List[(Item, Option[Long])]] = {
      val ttlName = dynamoQuery.request.table.ttlField
        .getOrElse(throw new RuntimeException(s"TTL field for table=${dynamoQuery.request.table.fullName} not specified but requested."))
      dynamoQuery
        .modify(
          _.withProjectionExpression(AttributeNames[Item].projectionExpression)
            .withProjectionExpression(ttlName)
        ).decodeF(FnBIO {
          response => implicit F =>
            import scala.jdk.CollectionConverters._

            val itemsData = ev.items(response).asScala.toList
            F.traverse(itemsData)(decodeItemTTLImpl(ttlName)(_)).map(_.flatten)
        })
    }

  }

  implicit final class DecodeItemAttributes[DR <: DynamoRequest, Dec](
    dynamoQuery: DynamoQuery[DR, Dec]
  )(implicit ev: HasAttributes[DR#Rsp]
  ) {
    def decodeItem[Item: D4SDecoder]: DynamoQuery[DR, Option[Item]] = {
      dynamoQuery.decodeF(FnBIO {
        response => implicit F =>
          decodeItemImpl(ev.attributes(response))
      })
    }
  }

  implicit final class DecodeItem[DR <: DynamoRequest with WithProjectionExpression[DR] with WithTableReference[DR], Dec](
    dynamoQuery: DynamoQuery[DR, Dec]
  )(implicit ev: HasItem[DR#Rsp]
  ) {
    def decodeItem[Item: D4SDecoder: AttributeNames]: DynamoQuery[DR, Option[Item]] = {
      dynamoQuery
        .modify(
          _.withProjectionExpression(AttributeNames[Item].projectionExpression)
        ).decodeF(FnBIO {
          response => implicit F =>
            decodeItemImpl(ev.item(response))
        })
    }

    def decodeItemWithTTL[Item: D4SDecoder: AttributeNames]: DynamoQuery[DR, Option[(Item, Option[Long])]] = {
      val ttlName = dynamoQuery.request.table.ttlField
        .getOrElse(throw new RuntimeException(s"TTL field for table=${dynamoQuery.request.table.fullName} not specified but requested."))

      dynamoQuery
        .modify(
          _.withProjectionExpression(AttributeNames[Item].projectionExpression)
            .withProjectionExpression(ttlName)
        ).decodeF(FnBIO {
          response => implicit F =>
            decodeItemTTLImpl(ttlName)(ev.item(response))
        })
    }

    def decodeItemCheckTTL[Item: D4SDecoder: AttributeNames](
      now: ZonedDateTime
    ): DynamoQuery[DR, Option[Item]] = {
      decodeItemCheckTTL(now.toEpochSecond)
    }

    def decodeItemCheckTTL[Item: D4SDecoder: AttributeNames](
      nowEpochSeconds: Long
    ): DynamoQuery[DR, Option[Item]] = {
      decodeItemWithTTL.decodeWith {
        case (_, Some((item, ttl))) if ttl.forall(_ >= nowEpochSeconds) => Some(item)
        case (_, _)                                                     => None
      }
    }
  }

  @inline private[this] def decodeItemImpl[F[+_, +_]: BIOError, Item: D4SDecoder](
    itemJavaMap: java.util.Map[String, AttributeValue]
  ): F[DecoderException, Option[Item]] = {
    if (!itemJavaMap.isEmpty) {
      F.fromEither(D4SDecoder[Item].decodeObject(itemJavaMap).map(Some(_)))
    } else {
      F.pure(None)
    }
  }

  @inline private[this] def decodeItemTTLImpl[F[+_, +_]: BIOError, Item: D4SDecoder](
    ttlName: String
  )(itemJavaMap: java.util.Map[String, AttributeValue]
  ): F[DecoderException, Option[(Item, Option[Long])]] = {
    if (!itemJavaMap.isEmpty) {
      F.fromEither {
        for {
          item <- D4SDecoder[Item].decodeObject(itemJavaMap)
          ttl   = Option(itemJavaMap.get(ttlName)).flatMap(i => Option(i.n()).map(_.toLong))
        } yield Some((item, ttl))
      }
    } else {
      F.pure(None)
    }
  }

}
