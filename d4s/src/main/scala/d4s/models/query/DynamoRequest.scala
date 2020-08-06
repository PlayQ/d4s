package d4s.models.query

import d4s.codecs.D4SEncoder
import d4s.models.conditions.Condition
import d4s.models.table.index.TableIndex
import d4s.models.table.{DynamoField, TablePrefix, TableReference}
import software.amazon.awssdk.services.dynamodb.model._

import scala.jdk.CollectionConverters._

trait DynamoRequest {
  type Rq <: DynamoDbRequest
  type Rsp
  def toAmz: Rq
}

object DynamoRequest {
  type Aux[Rq0, Rsp0] = DynamoRequest { type Rq = Rq0; type Rsp = Rsp0 }

  implicit final class ToQuery[DR <: DynamoRequest](private val dr: DR) extends AnyVal {
    def toQuery: DynamoQuery[DR, DR#Rsp] = DynamoQuery(dr)
  }

  private[d4s] trait DynamoWriteBatchRequest extends DynamoRequest {
    override type Rq  = BatchWriteItemRequest
    override type Rsp = List[BatchWriteItemResponse]
  }

  final case class BatchWriteEntity[T](item: T, ttl: Option[Long] = None)

  trait PageableRequest[DR <: DynamoRequest] {
    type PageMarker

    def withPageMarker(rq: DR, pageMarker: PageMarker): DR
    def getPageMarker(rsp: DR#Rsp): Option[PageMarker]

    final def withPageMarkerOption(rq: DR, pageMarker: Option[PageMarker]): DR = pageMarker.fold(rq)(withPageMarker(rq, _))
  }
  object PageableRequest {
    def apply[DR <: DynamoRequest]: ApplyPartiallyApplied[DR] = new ApplyPartiallyApplied[DR]

    private[PageableRequest] final class ApplyPartiallyApplied[DR <: DynamoRequest](private val dummy: Boolean = false) extends AnyVal {
      def apply[PageMarker0](getter: DR#Rsp => Option[PageMarker0])(setter: (DR, PageMarker0) => DR): PageableRequest[DR] = new PageableRequest[DR] {
        override type PageMarker = PageMarker0
        override def withPageMarker(rq: DR, pageMarker: PageMarker): DR = setter(rq, pageMarker)
        override def getPageMarker(rsp: DR#Rsp): Option[PageMarker]     = getter(rsp)
      }
    }
  }

  trait WithTableReference[A] {
    def table: TableReference
    def withTableReference(t: TableReference => TableReference): A
    final def withPrefix[TP: TablePrefix](prefix: TP): A = withTableReference(_.withPrefix(prefix))
  }

  trait WithIndex[A] {
    def withIndex(index: TableIndex[_, _]): A
  }

  trait WithCondition[A] {
    def withCondition(c: Condition): A
  }

  trait WithFilterExpression[A] {
    def withFilterExpression(expr: Condition): A
  }

  trait WithProjectionExpression[A] {
    def withProjectionExpression(f: Option[String] => Option[String]): A

    final def withProjectionExpression(expr: String): A            = withProjectionExpression(p => Some(p.fold("")(existingExpr => s"$existingExpr, ") + expr))
    final def withProjectionExpression(fields: DynamoField[_]*): A = withProjectionExpression(fields.mkString("", ", ", ""))
  }

  trait WithUpdateExpression[A] {
    def withUpdateExpression(f: String => String): A

    final def withUpdateExpression(expr: String): A = withUpdateExpression(existingExpr => s"$existingExpr $expr")
  }

  trait WithAttributeValues[A] {
    def withAttributeValues(f: Map[String, AttributeValue] => Map[String, AttributeValue]): A

    final def withAttributeValues(add: Map[String, AttributeValue]): A          = withAttributeValues(_ ++ add)
    final def withAttributeValues(value: (String, AttributeValue)): A           = withAttributeValues(_ + value)
    final def withAttribute[Item: D4SEncoder](value: Item): A                   = withAttributeValues(D4SEncoder[Item].encodeObject(value))
    final def withAttributes[I1: D4SEncoder, I2: D4SEncoder](v1: I1, v2: I2): A = withAttributeValues(D4SEncoder[I1].encodeObject(v1) ++ D4SEncoder[I2].encodeObject(v2))
  }

  trait WithAttributeNames[A] {
    def withAttributeNames(an: Map[String, String] => Map[String, String]): A

    final def withAttributeNames(add: Map[String, String]): A = withAttributeNames(_ ++ add)
    final def withAttributeNames(value: (String, String)): A  = withAttributeNames(_ + value)
  }

  trait WithLimit[A] {
    def withLimit(l: Int): A
  }

  trait WithSelect[A] {
    def withSelect(newSelect: Select): A
  }

  trait WithReturnValue[A] {
    def withReturnValue(newReturnValue: ReturnValue): A
  }

  trait WithStartKey[A] {
    def withStartKeyMap(startKey: java.util.Map[String, AttributeValue]): A
    final def withStartKey[Key: D4SEncoder](startKey: Key): A = withStartKeyMap(D4SEncoder[Key].encodeObject(startKey).asJava)
  }

  trait WithConsistent[A] {
    def withConsistent(consistentRead: Boolean): A
  }

  trait WithBatch[A, BatchType[_]] {
    def batchItems: List[Map[String, AttributeValue]]
    /** overwrites batch list, subsequent calls do not append */
    def withBatch[I: D4SEncoder](batchItems: List[BatchType[I]]): A
    /** overwrites batch list, subsequent calls do not append */
    def withBatch(batchItems: List[Map[String, AttributeValue]]): A
  }

  trait WithScanIndexForward[A] {
    def withScanIndexForward(sif: Boolean): A
  }

  trait WithKey[A] {
    def withKey(f: Map[String, AttributeValue] => Map[String, AttributeValue]): A

    final def withKey(add: Map[String, AttributeValue]): A        = withKey(_ ++ add)
    final def withKey(value: (String, AttributeValue)): A         = withKey(_ + value)
    final def withKeyField[T](field: DynamoField[T])(value: T): A = withKey(_ + field.bind(value))
    final def withKeyItem[Item: D4SEncoder](value: Item): A       = withKey(_ ++ D4SEncoder[Item].encodeObject(value))
  }

  trait WithItem[A] {
    def withItemAttributeValues(f: Map[String, AttributeValue] => Map[String, AttributeValue]): A

    final def withItemAttributeValues(add: Map[String, AttributeValue]): A = withItemAttributeValues(_ ++ add)
    final def withItemAttributeValues(value: (String, AttributeValue)): A  = withItemAttributeValues(_ + value)
    final def withItemField[T](field: DynamoField[T])(value: T): A         = withItemAttributeValues(_ + field.bind(value))
    final def withItem[Item: D4SEncoder](value: Item): A                   = withItemAttributeValues(D4SEncoder[Item].encodeObject(value))
    final def withItems[I1: D4SEncoder, I2: D4SEncoder](v1: I1, v2: I2): A = withItemAttributeValues(D4SEncoder[I1].encodeObject(v1) ++ D4SEncoder[I2].encodeObject(v2))
  }

  trait WithParallelism[A] {
    def maxParallelDeletes: Option[Int]
    def withParallelism(parallelism: Int): A
  }

  trait WithWrappedRequest[Wrapped] {
    def wrapped: Wrapped
  }
}
