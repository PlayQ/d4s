package d4s.models.query.requests

import java.util

import d4s.models.conditions.Condition
import d4s.models.conditions.Condition.ZeroCondition
import d4s.models.query.DynamoRequest
import d4s.models.query.DynamoRequest._
import d4s.models.table.TableReference
import d4s.models.table.index.TableIndex
import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, BatchWriteItemResponse, ScanRequest, Select}

final case class ScanDeleteBatch(
  table: TableReference,
  maxParallelDeletes: Option[Int]                         = None,
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
  with WithFilterExpression[ScanDeleteBatch]
  with WithAttributeValues[ScanDeleteBatch]
  with WithAttributeNames[ScanDeleteBatch]
  with WithProjectionExpression[ScanDeleteBatch]
  with WithSelect[ScanDeleteBatch]
  with WithStartKey[ScanDeleteBatch]
  with WithLimit[ScanDeleteBatch]
  with WithTableReference[ScanDeleteBatch]
  with WithIndex[ScanDeleteBatch]
  with WithConsistent[ScanDeleteBatch]
  with WithParallelism[ScanDeleteBatch]
  with WithWrappedRequest[Scan] {

  override type Rq  = ScanRequest
  override type Rsp = List[BatchWriteItemResponse]

  override def withParallelism(parallelism: Int): ScanDeleteBatch = copy(maxParallelDeletes = Some(parallelism))

  override def withAttributeNames(an: Map[String, String] => Map[String, String]): ScanDeleteBatch = copy(attributeNames = an(attributeNames))

  override def withConsistent(consistentRead: Boolean): ScanDeleteBatch = copy(consistent = consistentRead)

  override def withSelect(newSelect: Select): ScanDeleteBatch = copy(select = Some(newSelect))

  override def withIndex(index: TableIndex[_, _]): ScanDeleteBatch = copy(index = Some(index.name))

  override def withStartKeyMap(startKey: util.Map[String, AttributeValue]): ScanDeleteBatch = copy(startKey = Some(startKey))

  override def withTableReference(t: TableReference => TableReference): ScanDeleteBatch = copy(table = t(table))

  override def withProjectionExpression(f: Option[String] => Option[String]): ScanDeleteBatch = copy(projectionExpression = f(projectionExpression))

  override def withFilterExpression(t: Condition): ScanDeleteBatch = copy(filterExpression = filterExpression && t)

  override def withAttributeValues(f: Map[String, AttributeValue] => Map[String, AttributeValue]): ScanDeleteBatch = copy(attributeValues = f(attributeValues))

  override def withLimit(l: Int): ScanDeleteBatch = copy(limit = Some(l))

  override def toAmz: ScanRequest = wrapped.toAmz

  override def wrapped: Scan = {
    Scan(
      table                = table,
      index                = index,
      filterExpression     = filterExpression,
      attributeValues      = attributeValues,
      attributeNames       = attributeNames,
      projectionExpression = projectionExpression,
      limit                = limit,
      select               = select,
      startKey             = startKey,
      consistent           = consistent,
    )
  }
}
