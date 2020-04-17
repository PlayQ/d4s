package d4s.models.query

import java.time.ZonedDateTime

import d4s.codecs.CodecsUtils.DynamoDecoderException
import d4s.codecs.{AttributeNames, D4SDecoder, D4SEncoder}
import d4s.config.ProvisionedThroughputConfig
import d4s.implicits._
import d4s.models.conditions.Condition
import d4s.models.conditions.Condition.{attribute_exists, attribute_not_exists}
import d4s.models.query.DynamoRequest._
import d4s.models.query.requests.UpdateTable
import d4s.models.table.index.{GlobalIndexUpdate, ProvisionedGlobalIndex, TableIndex}
import d4s.models.table.{DynamoField, TableReference}
import d4s.models.{DynamoExecution, FnBIO}
import d4s.util.OffsetLimit
import izumi.functional.bio.{BIO, BIOError, F}
import izumi.fundamentals.platform.language.unused
import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, ConsumedCapacity, ReturnValue, Select}

import scala.language.{implicitConversions, reflectiveCalls}

final case class DynamoQuery[DR <: DynamoRequest, +Dec](
  request: DR,
  decoder: FnBIO[DR#Rsp, Dec]
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

  @inline implicit final def toDynamoExecution[DR <: DynamoRequest, Dec](dynamoQuery: DynamoQuery[DR, Dec]): DynamoExecution[DR, Dec, Dec] = {
    DynamoExecution(dynamoQuery, DynamoExecution.single[DR, Dec])
  }

  implicit final class Exec[DR <: DynamoRequest, Dec](private val dynamoQuery: DynamoQuery[DR, Dec]) extends AnyVal {
    def exec: DynamoExecution[DR, Dec, Dec] = toDynamoExecution(dynamoQuery)
  }

  implicit final class ExecPagedFlatten[DR <: DynamoRequest: PageableRequest, Dec: ? <:< List[A], A](dynamoQuery: DynamoQuery[DR, Dec]) {
    def execPagedFlatten(limit: Option[Int] = None): DynamoExecution[DR, Dec, List[A]] = {
      DynamoExecution(dynamoQuery, DynamoExecution.pagedFlatten[DR, Dec, A](limit))
    }
  }

  implicit final class ExecPaged[DR <: DynamoRequest: PageableRequest, Dec](dynamoQuery: DynamoQuery[DR, Dec]) {
    def execPaged(limit: Option[Int] = None): DynamoExecution[DR, Dec, List[Dec]] = {
      DynamoExecution(dynamoQuery, DynamoExecution.paged[DR, Dec](limit))
    }

    def execStreamed: DynamoExecution.Streamed[DR, Dec, Dec] = {
      DynamoExecution.Streamed[DR, Dec, Dec](dynamoQuery, DynamoExecution.Streamed.streamed[DR, Dec])
    }
  }

  implicit final class ExecStreamFlatten[DR <: DynamoRequest: PageableRequest, Dec: ? <:< List[A], A](dynamoQuery: DynamoQuery[DR, Dec]) {
    def execStreamedFlatten: DynamoExecution.Streamed[DR, Dec, A] = {
      DynamoExecution.Streamed[DR, Dec, A](dynamoQuery, DynamoExecution.Streamed.streamedFlatten[DR, Dec, A])
    }
  }

  implicit final class ExecOffset[DR <: DynamoRequest, Dec, A](dynamoQuery: DynamoQuery[DR, Dec])(
    implicit
    paging: PageableRequest[DR],
    ev1: DR <:< WithSelect[DR] with WithLimit[DR] with WithProjectionExpression[DR],
    ev2: DR#Rsp => { def count(): Integer },
    ev4: Dec <:< List[A]
  ) {
    def execOffset(offsetLimit: OffsetLimit): DynamoExecution[DR, Dec, List[A]] = {
      new DynamoExecution[DR, Dec, List[A]](dynamoQuery, DynamoExecution.offset[DR, Dec, A](offsetLimit))
    }
  }

  implicit final class TweakFilterExpression[DR <: DynamoRequest, Dec](dynamoQuery: DynamoQuery[DR, Dec])(implicit ev: DR <:< WithFilterExpression[DR])
    extends WithFilterExpression[DynamoQuery[DR, Dec]] {
    @inline def withFilterExpression(c: Condition): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withFilterExpression(c))
    }
  }

  implicit final class TweakAttributeValues[DR <: DynamoRequest, Dec](dynamoQuery: DynamoQuery[DR, Dec])(implicit ev: DR <:< WithAttributeValues[DR])
    extends WithAttributeValues[DynamoQuery[DR, Dec]] {
    @inline def withAttributeValues(f: Map[String, AttributeValue] => Map[String, AttributeValue]): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withAttributeValues(f))
    }
  }

  implicit final class TweakAttributeNames[DR <: DynamoRequest, Dec](dynamoQuery: DynamoQuery[DR, Dec])(implicit ev: DR <:< WithAttributeNames[DR])
    extends WithAttributeNames[DynamoQuery[DR, Dec]] {
    @inline def withAttributeNames(f: Map[String, String] => Map[String, String]): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withAttributeNames(f))
    }
  }

  implicit final class TweakProjectionExpression[DR <: DynamoRequest, Dec](dynamoQuery: DynamoQuery[DR, Dec])(implicit ev: DR <:< WithProjectionExpression[DR])
    extends WithProjectionExpression[DynamoQuery[DR, Dec]] {
    @inline def withProjectionExpression(f: Option[String] => Option[String]): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withProjectionExpression(f))
    }
  }

  implicit final class TweakTableReference[DR <: DynamoRequest, Dec](dynamoQuery: DynamoQuery[DR, Dec])(implicit ev: DR <:< WithTableReference[DR])
    extends WithTableReference[DynamoQuery[DR, Dec]] {
    @inline def withTableReference(f: TableReference => TableReference): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withTableReference(f))
    }
    @inline def table: TableReference = dynamoQuery.request.table
  }

  implicit final class TweakIndex[DR <: DynamoRequest, Dec](dynamoQuery: DynamoQuery[DR, Dec])(implicit ev: DR <:< WithIndex[DR])
    extends WithIndex[DynamoQuery[DR, Dec]] {
    @inline override def withIndex(index: TableIndex[_, _]): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withIndex(index))
    }
  }

  implicit final class TweakLimit[DR <: DynamoRequest, Dec](dynamoQuery: DynamoQuery[DR, Dec])(implicit ev: DR <:< WithLimit[DR])
    extends WithLimit[DynamoQuery[DR, Dec]] {
    @inline override def withLimit(limit: Int): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withLimit(limit))
    }
  }

  implicit final class TweakStartKey[DR <: DynamoRequest, Dec](dynamoQuery: DynamoQuery[DR, Dec])(implicit ev: DR <:< WithStartKey[DR])
    extends WithStartKey[DynamoQuery[DR, Dec]] {
    @inline override def withStartKeyMap(startKey: java.util.Map[String, AttributeValue]): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withStartKeyMap(startKey))
    }
  }

  implicit final class TweakBatchItems[DR <: DynamoRequest, BatchType[_], Dec](dynamoQuery: DynamoQuery[DR, Dec])(implicit ev: DR <:< WithBatch[DR, BatchType])
    extends WithBatch[DynamoQuery[DR, Dec], BatchType] {
    @inline override def withBatch[I: D4SEncoder](batchItems: List[BatchType[I]]): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withBatch(batchItems))
    }
    @inline override def withBatch(batchItems: List[Map[String, AttributeValue]]): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withBatch(batchItems))
    }
    @inline override def batchItems: List[Map[String, AttributeValue]] = dynamoQuery.request.batchItems
  }

  implicit final class TweakScanIndexForward[DR <: DynamoRequest, Dec](dynamoQuery: DynamoQuery[DR, Dec])(implicit ev: DR <:< WithScanIndexForward[DR])
    extends WithScanIndexForward[DynamoQuery[DR, Dec]] {
    @inline override def withScanIndexForward(sif: Boolean): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withScanIndexForward(sif))
    }
  }

  implicit final class TweakKey[DR <: DynamoRequest, Dec](dynamoQuery: DynamoQuery[DR, Dec])(implicit ev: DR <:< WithKey[DR]) extends WithKey[DynamoQuery[DR, Dec]] {
    @inline override def withKey(f: Map[String, AttributeValue] => Map[String, AttributeValue]): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withKey(f))
    }
  }

  implicit final class TweakItem[DR <: DynamoRequest, Dec](dynamoQuery: DynamoQuery[DR, Dec])(implicit ev: DR <:< WithItem[DR]) extends WithItem[DynamoQuery[DR, Dec]] {
    @inline override def withItemAttributeValues(f: Map[String, AttributeValue] => Map[String, AttributeValue]): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withItemAttributeValues(f))
    }
  }

  @SuppressWarnings(Array("UnsafeTraversableMethods"))
  implicit final class TweakExists[DR <: DynamoRequest, Dec](dynamoQuery: DynamoQuery[DR, Dec])(implicit ev: DR <:< WithCondition[DR] with WithTableReference[DR]) {
    def ifExists(): DynamoQuery[DR, Dec] = {
      val table = dynamoQuery.table
      dynamoQuery.withCondition(table.key.keyNames.toList.map(s => attribute_exists(List(s)): Condition).reduceLeft(_ && _))
    }

    def ifNotExists(): DynamoQuery[DR, Dec] = {
      val table = dynamoQuery.table
      dynamoQuery.withCondition(table.key.keyNames.toList.map(s => attribute_not_exists(List(s)): Condition).reduceLeft(_ && _))
    }
  }

  implicit final class TweakCondition[DR <: DynamoRequest, Dec](dynamoQuery: DynamoQuery[DR, Dec])(implicit ev: DR <:< WithCondition[DR])
    extends WithCondition[DynamoQuery[DR, Dec]] {
    @inline def withCondition(c: Condition): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withCondition(c))
    }
  }

  implicit final class TweakUpdateExpression[DR <: DynamoRequest, Dec](dynamoQuery: DynamoQuery[DR, Dec])(implicit ev: DR <:< WithUpdateExpression[DR])
    extends WithUpdateExpression[DynamoQuery[DR, Dec]] {
    @inline def withUpdateExpression(f: String => String): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withUpdateExpression(f))
    }
  }

  implicit final class TweakWithTtl[DR <: DynamoRequest, Dec](dynamoQuery: DynamoQuery[DR, Dec])(
    implicit ev: DR <:< WithAttributeValues[DR] with WithTableReference[DR] with WithFilterExpression[DR]
  ) {
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

  implicit final class TweakWithTtlField[DR <: DynamoRequest, Dec](private val dynamoQuery: DynamoQuery[DR, Dec]) extends AnyVal {
    def withTtlFieldOption(expiration: Option[ZonedDateTime])(implicit ev: DR <:< WithItem[DR] with WithTableReference[DR]): DynamoQuery[DR, Dec] =
      expiration.fold(dynamoQuery)(withTtlField(_))

    def withTtlFieldOption(expirationEpochSeconds: Option[Long])(implicit ev: DR <:< WithItem[DR] with WithTableReference[DR],
                                                                 @unused dummy: DummyImplicit): DynamoQuery[DR, Dec] = {
      expirationEpochSeconds.fold(dynamoQuery)(withTtlField(_))
    }

    def withTtlField(expiration: ZonedDateTime)(implicit ev: DR <:< WithItem[DR] with WithTableReference[DR]): DynamoQuery[DR, Dec] =
      withTtlField(expiration.toEpochSecond)

    def withTtlField(expirationEpochSeconds: Long)(implicit ev: DR <:< WithItem[DR] with WithTableReference[DR]): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify {
        rq =>
          rq.table.ttlField.fold(rq) {
            t =>
              rq.withItemField(DynamoField[Long](t))(expirationEpochSeconds)
          }
      }
    }
  }

  implicit final class TweakWithConsistent[DR <: DynamoRequest, Dec](dynamoQuery: DynamoQuery[DR, Dec])(implicit ev: DR <:< WithConsistent[DR])
    extends WithConsistent[DynamoQuery[DR, Dec]] {
    override def withConsistent(consistentRead: Boolean): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withConsistent(consistentRead))
    }

    def consistent: DynamoQuery[DR, Dec] = withConsistent(true)
  }

  implicit final class TweakReturnValue[DR <: DynamoRequest, Dec](dynamoQuery: DynamoQuery[DR, Dec])(implicit ev: DR <:< WithReturnValue[DR])
    extends WithReturnValue[DynamoQuery[DR, Dec]] {
    override def withReturnValue(returnValue: ReturnValue): DynamoQuery[DR, Dec] = {
      dynamoQuery.modify(_.withReturnValue(returnValue))
    }
  }

  implicit final class UpdateOps[Dec](private val dynamoQuery: DynamoQuery[UpdateTable, Dec]) extends AnyVal {
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

  implicit final class QueryCount[DR <: DynamoRequest, Rb, Dec](private val dynamoQuery: DynamoQuery[DR, Dec]) extends AnyVal {
    def countOnly(implicit ev1: DR <:< WithSelect[DR] with WithProjectionExpression[DR], ev3: DR#Rsp => { def count(): Integer }): DynamoQuery[DR, Int] =
      dynamoQuery.modify(_.withSelect(Select.COUNT).withProjectionExpression(_ => None)).decode(_.count())

    def scannedCountOnly(implicit ev1: DR <:< WithSelect[DR] with WithProjectionExpression[DR], ev2: DR#Rsp => { def scannedCount(): Integer }): DynamoQuery[DR, Int] =
      dynamoQuery.modify(_.withSelect(Select.COUNT).withProjectionExpression(_ => None)).decode(_.scannedCount())

    def consumedCapacityOnly(implicit ev4: DR#Rsp => { def consumedCapacity(): ConsumedCapacity }): DynamoQuery[DR, ConsumedCapacity] =
      dynamoQuery.decode(_.consumedCapacity())

    def withCount(implicit ev3: DR#Rsp => { def count(): Integer }): DynamoQuery[DR, (Dec, Int)] =
      dynamoQuery.decodeWith((a, c) => (c, a.count()))

    def withScannedCount(implicit ev2: DR#Rsp => { def scannedCount(): Integer }): DynamoQuery[DR, (Dec, Int)] =
      dynamoQuery.decodeWith((a, c) => (c, a.scannedCount()))

    def withConsumedCapacity(implicit ev4: DR#Rsp => { def consumedCapacity(): ConsumedCapacity }): DynamoQuery[DR, (Dec, ConsumedCapacity)] =
      dynamoQuery.decodeWith((a, c) => (c, a.consumedCapacity()))
  }

  implicit final class DecodeItems[DR <: DynamoRequest, Rb, Dec](dynamoQuery: DynamoQuery[DR, Dec])(
    implicit
    ev1: DR <:< WithProjectionExpression[DR] with WithTableReference[DR],
    ev3: DR#Rsp => { def items(): java.util.List[java.util.Map[String, AttributeValue]] }
  ) {
    def decodeItems[Item: D4SDecoder: AttributeNames]: DynamoQuery[DR, List[Item]] = {
      dynamoQuery
        .modify(
          _.withProjectionExpression(AttributeNames[Item].projectionExpression)
        ).decodeF(FnBIO {
          response => implicit F =>
            import scala.jdk.CollectionConverters._

            val itemsData = response.items().asScala.toList
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

            val itemsData = response.items().asScala.toList
            F.traverse(itemsData)(decodeItemTTLImpl(ttlName)(_)).map(_.flatten)
        })
    }

  }

  implicit final class DecodeBatchedItems[DR <: DynamoRequest, Rb, Dec, A](
    dynamoQuery: DynamoQuery[DR, Dec]
  )(implicit
    ev1: DR#Rsp <:< List[A],
    ev2: A => { def responses(): java.util.Map[String, java.util.List[java.util.Map[String, AttributeValue]]] }) {

    def decodeItems[Item: D4SDecoder]: DynamoQuery[DR, List[Item]] = {
      dynamoQuery.decodeF(FnBIO {
        rsp => implicit F =>
          import scala.jdk.CollectionConverters._

          val itemsData = rsp.flatMap(_.responses().asScala.values.flatMap(_.asScala).toList)
          F.traverse(itemsData)(decodeItemImpl(_)).map(_.flatten)
      })
    }
  }

  implicit final class DecodeItem[DR <: DynamoRequest, Rb, Dec](dynamoQuery: DynamoQuery[DR, Dec])(implicit
                                                                                                   ev1: DR <:< WithProjectionExpression[DR] with WithTableReference[DR],
                                                                                                   ev2: DR#Rsp => { def item(): java.util.Map[String, AttributeValue] }) {
    def decodeItem[Item: D4SDecoder: AttributeNames]: DynamoQuery[DR, Option[Item]] = {
      dynamoQuery
        .modify(
          _.withProjectionExpression(AttributeNames[Item].projectionExpression)
        ).decodeF(FnBIO {
          response => implicit F =>
            decodeItemImpl(response.item())
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
            decodeItemTTLImpl(ttlName)(response.item())
        })
    }

    def decodeItemCheckTTL[Item: D4SDecoder: AttributeNames](now: ZonedDateTime): DynamoQuery[DR, Option[Item]] = {
      decodeItemCheckTTL(now.toEpochSecond)
    }

    def decodeItemCheckTTL[Item: D4SDecoder: AttributeNames](nowEpochSeconds: Long): DynamoQuery[DR, Option[Item]] = {
      decodeItemWithTTL.decodeWith {
        case (_, Some((item, ttl))) if ttl.forall(_ >= nowEpochSeconds) => Some(item)
        case (_, _)                                                     => None
      }
    }
  }

  implicit final class DecodeItemAttributes[DR <: DynamoRequest, Rb, Dec](dynamoQuery: DynamoQuery[DR, Dec])(implicit
                                                                                                             ev: DR#Rsp => {
                                                                                                               def attributes(): java.util.Map[String, AttributeValue]
                                                                                                             }) {
    def decodeItem[Item: D4SDecoder]: DynamoQuery[DR, Option[Item]] = {
      dynamoQuery.decodeF(FnBIO {
        response => implicit F =>
          decodeItemImpl(response.attributes())
      })
    }
  }

  @inline private[this] def decodeItemImpl[F[+_, +_]: BIOError, Item: D4SDecoder](
    itemJavaMap: java.util.Map[String, AttributeValue]
  ): F[DynamoDecoderException, Option[Item]] = {
    if (!itemJavaMap.isEmpty) {
      F.fromEither(D4SDecoder[Item].decode(itemJavaMap).map(Some(_)))
    } else {
      F.pure(None)
    }
  }

  @inline private[this] def decodeItemTTLImpl[F[+_, +_]: BIOError, Item: D4SDecoder](ttlName: String)(
    itemJavaMap: java.util.Map[String, AttributeValue]
  ): F[DynamoDecoderException, Option[(Item, Option[Long])]] = {
    if (!itemJavaMap.isEmpty) {
      F.fromEither {
        for {
          item <- D4SDecoder[Item].decode(itemJavaMap)
          ttl  = Option(itemJavaMap.get(ttlName)).flatMap(i => Option(i.n()).map(_.toLong))
        } yield Some((item, ttl))
      }
    } else {
      F.pure(None)
    }
  }

}
