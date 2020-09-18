package d4s.models.query.requests

import java.util

import d4s.models.conditions.Condition
import d4s.models.conditions.Condition.ZeroCondition
import d4s.models.query.DynamoRequest
import d4s.models.query.DynamoRequest._
import d4s.models.table.TableReference
import d4s.models.table.index.TableIndex
import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, ScanRequest, ScanResponse, Select}

import scala.util.chaining._

final case class Scan(
  table: TableReference,
  index: Option[String]                                   = None,
  filterExpression: Condition                             = ZeroCondition,
  attributeValues: Map[String, AttributeValue]            = Map.empty,
  attributeNames: Map[String, String]                     = Map.empty,
  projectionExpression: Option[String]                    = None,
  limit: Option[Int]                                      = None,
  select: Option[Select]                                  = None,
  startKey: Option[java.util.Map[String, AttributeValue]] = None,
  consistent: Boolean                                     = false,
) extends DynamoRequest
  with WithFilterExpression[Scan]
  with WithAttributeValues[Scan]
  with WithAttributeNames[Scan]
  with WithProjectionExpression[Scan]
  with WithSelect[Scan]
  with WithStartKey[Scan]
  with WithLimit[Scan]
  with WithTableReference[Scan]
  with WithIndex[Scan]
  with WithConsistent[Scan] {

  override type Rq  = ScanRequest
  override type Rsp = ScanResponse

  override def withAttributeNames(an: Map[String, String] => Map[String, String]): Scan = copy(attributeNames = an(attributeNames))

  override def withConsistent(consistentRead: Boolean): Scan = copy(consistent = consistentRead)

  override def withSelect(newSelect: Select): Scan = copy(select = Some(newSelect))

  override def withIndex(index: TableIndex[_, _]): Scan = copy(index = Some(index.name))

  override def withStartKeyMap(startKey: util.Map[String, AttributeValue]): Scan = copy(startKey = Some(startKey))

  override def withTableReference(t: TableReference => TableReference): Scan = copy(table = t(table))

  override def withProjectionExpression(f: Option[String] => Option[String]): Scan = copy(projectionExpression = f(projectionExpression))

  override def withFilterExpression(t: Condition): Scan = copy(filterExpression = filterExpression && t)

  override def withAttributeValues(f: Map[String, AttributeValue] => Map[String, AttributeValue]): Scan = copy(attributeValues = f(attributeValues))

  override def withLimit(l: Int): Scan = copy(limit = Some(l))

  override def toAmz: ScanRequest = {
    val evaluatedFilter = filterExpression.eval

    ScanRequest
      .builder()
      .tableName(table.fullName)
      .expressionAttributeValues(evaluatedFilter.withAttributes(attributeValues))
      .expressionAttributeNames(evaluatedFilter.withAliases(attributeNames))
      .filterExpression(evaluatedFilter.conditionExpression.orNull)
      .pipe(b => index.fold(b)(b.indexName))
      .pipe(b => projectionExpression.fold(b)(b.projectionExpression))
      .pipe(b => limit.fold(b)(b.limit(_)))
      .pipe(b => select.fold(b)(b.select))
      .pipe(b => startKey.fold(b)(b.exclusiveStartKey))
      .consistentRead(consistent)
      .build()
  }
}
object Scan {
  implicit val pageableRequest: PageableRequest[Scan] = PageableRequest[Scan](rsp => Option(rsp.lastEvaluatedKey()).filter(!_.isEmpty))(_.withStartKeyMap(_))
}
