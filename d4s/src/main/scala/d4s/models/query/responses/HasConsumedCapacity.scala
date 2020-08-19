package d4s.models.query.responses

import software.amazon.awssdk.services.dynamodb.model._

trait HasConsumedCapacity[A] {
  def consumedCapacity(a: A): ConsumedCapacity
}

object HasConsumedCapacity {
  @inline def apply[A: HasConsumedCapacity]: HasConsumedCapacity[A] = implicitly

  implicit val hasConsumedCapacityDeleteItemResponse: HasConsumedCapacity[DeleteItemResponse] = _.consumedCapacity()
  implicit val hasConsumedCapacityGetItemResponse: HasConsumedCapacity[GetItemResponse]       = _.consumedCapacity()
  implicit val hasConsumedCapacityPutItemResponse: HasConsumedCapacity[PutItemResponse]       = _.consumedCapacity()
  implicit val hasConsumedCapacityUpdateItemResponse: HasConsumedCapacity[UpdateItemResponse] = _.consumedCapacity()
  implicit val hasConsumedCapacityQueryResponse: HasConsumedCapacity[QueryResponse]           = _.consumedCapacity()
  implicit val hasConsumedCapacityScanResponse: HasConsumedCapacity[ScanResponse]             = _.consumedCapacity()
}
