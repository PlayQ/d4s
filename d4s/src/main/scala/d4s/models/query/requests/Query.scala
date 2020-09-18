package d4s.models.query.requests

import java.util

import d4s.models.conditions.Condition._
import d4s.models.conditions.{Condition, LogicalOperator}
import d4s.models.query.DynamoRequest
import d4s.models.query.DynamoRequest._
import d4s.models.table.TableReference
import d4s.models.table.index.TableIndex
import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, QueryRequest, QueryResponse, Select}

import scala.util.chaining._

final case class Query(
  table: TableReference,
  index: Option[String]                                    = None,
  filterExpression: Condition                              = ZeroCondition,
  projectionExpression: Option[String]                     = None,
  limit: Option[Int]                                       = None,
  select: Option[Select]                                   = None,
  startKey: Option[java.util.Map[String, AttributeValue]]  = None,
  consistent: Boolean                                      = false,
  scanIndexForward: Boolean                                = false,
  attributeValues: Map[String, AttributeValue]             = Map.empty,
  attributeNames: Map[String, String]                      = Map.empty,
  condition: Condition                                     = ZeroCondition,
  keyConditionAttributeValues: Map[String, AttributeValue] = Map.empty,
) extends DynamoRequest
  with WithFilterExpression[Query]
  with WithAttributeValues[Query]
  with WithAttributeNames[Query]
  with WithProjectionExpression[Query]
  with WithSelect[Query]
  with WithStartKey[Query]
  with WithTableReference[Query]
  with WithIndex[Query]
  with WithLimit[Query]
  with WithConsistent[Query]
  with WithScanIndexForward[Query]
  with WithCondition[Query]
  with WithKey[Query] {

  override type Rq  = QueryRequest
  override type Rsp = QueryResponse

  override def withCondition(t: Condition): Query = copy(condition = condition && t)

  override def withAttributeNames(an: Map[String, String] => Map[String, String]): Query = copy(attributeNames = an(attributeNames))

  override def withScanIndexForward(sif: Boolean): Query = copy(scanIndexForward = sif)

  override def withIndex(index: TableIndex[_, _]): Query = copy(index = Some(index.name))

  override def withStartKeyMap(startKey: util.Map[String, AttributeValue]): Query = copy(startKey = Some(startKey))

  override def withSelect(newSelect: Select): Query = copy(select = Some(newSelect))

  override def withTableReference(t: TableReference => TableReference): Query = copy(table = t(table))

  override def withProjectionExpression(f: Option[String] => Option[String]): Query = copy(projectionExpression = f(projectionExpression))

  override def withFilterExpression(t: Condition): Query = copy(filterExpression = filterExpression && t)

  override def withAttributeValues(f: Map[String, AttributeValue] => Map[String, AttributeValue]): Query = copy(attributeValues = f(attributeValues))

  override def withLimit(l: Int): Query = copy(limit = Some(l))

  override def withConsistent(consistentRead: Boolean): Query = copy(consistent = consistentRead)

  override def withKey(f: Map[String, AttributeValue] => Map[String, AttributeValue]): Query = copy(keyConditionAttributeValues = f(keyConditionAttributeValues))

  override def toAmz: QueryRequest = {
    val evaluatedCondition =
      (condition :: keyConditionAttributeValues.toList.map {
        case (k, v) =>
          logical(List(k), LogicalOperator.==, v)
      }).foldLeft[Condition](ZeroCondition)(and).eval

    val evaluatedFilter = filterExpression.eval

    QueryRequest
      .builder()
      .tableName(table.fullName)
      .keyConditionExpression(evaluatedCondition.conditionExpression.orNull)
      .filterExpression(evaluatedFilter.conditionExpression.orNull)
      .expressionAttributeValues(evaluatedCondition.withAttributes(evaluatedFilter.attrValues ++ attributeValues))
      .expressionAttributeNames(evaluatedCondition.withAliases(evaluatedFilter.aliases ++ attributeNames))
      .scanIndexForward(scanIndexForward)
      .pipe(b => index.fold(b)(b.indexName))
      .pipe(b => projectionExpression.fold(b)(b.projectionExpression))
      .pipe(b => limit.fold(b)(b.limit(_)))
      .pipe(b => select.fold(b)(b.select))
      .pipe(b => startKey.fold(b)(b.exclusiveStartKey))
      .consistentRead(consistent)
      .build()
  }
}
object Query {
  implicit val pageableRequest: PageableRequest[Query] = PageableRequest[Query](rsp => Option(rsp.lastEvaluatedKey()).filter(!_.isEmpty))(_.withStartKeyMap(_))
}
