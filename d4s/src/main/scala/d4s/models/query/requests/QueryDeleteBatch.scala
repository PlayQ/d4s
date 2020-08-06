package d4s.models.query.requests

import java.util

import d4s.models.conditions.Condition
import d4s.models.conditions.Condition.ZeroCondition
import d4s.models.query.DynamoRequest
import d4s.models.query.DynamoRequest.{WithAttributeNames, WithAttributeValues, WithCondition, WithConsistent, WithFilterExpression, WithIndex, WithKey, WithLimit, WithParallelism, WithProjectionExpression, WithScanIndexForward, WithSelect, WithStartKey, WithTableReference, WithWrappedRequest}
import d4s.models.table.TableReference
import d4s.models.table.index.TableIndex
import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, BatchWriteItemResponse, QueryRequest, Select}

final case class QueryDeleteBatch(
  table: TableReference,
  maxParallelDeletes: Option[Int]                          = None,
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
  with WithFilterExpression[QueryDeleteBatch]
  with WithAttributeValues[QueryDeleteBatch]
  with WithAttributeNames[QueryDeleteBatch]
  with WithProjectionExpression[QueryDeleteBatch]
  with WithSelect[QueryDeleteBatch]
  with WithStartKey[QueryDeleteBatch]
  with WithIndex[QueryDeleteBatch]
  with WithLimit[QueryDeleteBatch]
  with WithConsistent[QueryDeleteBatch]
  with WithScanIndexForward[QueryDeleteBatch]
  with WithCondition[QueryDeleteBatch]
  with WithTableReference[QueryDeleteBatch]
  with WithKey[QueryDeleteBatch]
  with WithParallelism[QueryDeleteBatch]
  with WithWrappedRequest[Query] {

  override type Rq  = QueryRequest
  override type Rsp = List[BatchWriteItemResponse]

  override def withParallelism(parallelism: Int): QueryDeleteBatch = copy(maxParallelDeletes = Some(parallelism))

  override def withCondition(t: Condition): QueryDeleteBatch = copy(condition = condition && t)

  override def withAttributeNames(an: Map[String, String] => Map[String, String]): QueryDeleteBatch = copy(attributeNames = an(attributeNames))

  override def withScanIndexForward(sif: Boolean): QueryDeleteBatch = copy(scanIndexForward = sif)

  override def withIndex(index: TableIndex[_, _]): QueryDeleteBatch = copy(index = Some(index.name))

  override def withStartKeyMap(startKey: util.Map[String, AttributeValue]): QueryDeleteBatch = copy(startKey = Some(startKey))

  override def withSelect(newSelect: Select): QueryDeleteBatch = copy(select = Some(newSelect))

  override def withTableReference(t: TableReference => TableReference): QueryDeleteBatch = copy(table = t(table))

  override def withProjectionExpression(f: Option[String] => Option[String]): QueryDeleteBatch = copy(projectionExpression = f(projectionExpression))

  override def withFilterExpression(t: Condition): QueryDeleteBatch = copy(filterExpression = filterExpression && t)

  override def withAttributeValues(f: Map[String, AttributeValue] => Map[String, AttributeValue]): QueryDeleteBatch = copy(attributeValues = f(attributeValues))

  override def withLimit(l: Int): QueryDeleteBatch = copy(limit = Some(l))

  override def withConsistent(consistentRead: Boolean): QueryDeleteBatch = copy(consistent = consistentRead)

  override def withKey(f: Map[String, AttributeValue] => Map[String, AttributeValue]): QueryDeleteBatch =
    copy(keyConditionAttributeValues = f(keyConditionAttributeValues))

  override def toAmz: QueryRequest = wrapped.toAmz

  override def wrapped: Query = {
    Query(
      table                       = table,
      index                       = index,
      filterExpression            = filterExpression,
      projectionExpression        = projectionExpression,
      limit                       = limit,
      select                      = select,
      startKey                    = startKey,
      consistent                  = consistent,
      scanIndexForward            = scanIndexForward,
      attributeValues             = attributeValues,
      attributeNames              = attributeNames,
      condition                   = condition,
      keyConditionAttributeValues = keyConditionAttributeValues,
    )
  }

}
