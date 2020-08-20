package d4s.models.query.responses

import software.amazon.awssdk.services.dynamodb.model._

trait HasItem[A] {
  def item(a: A): java.util.Map[String, AttributeValue]
}

object HasItem {
  @inline def apply[A: HasItem]: HasItem[A] = implicitly

  implicit val hasItemGetItemResponse: HasItem[GetItemResponse]       = _.item()
  implicit val hasItemCancellationReason: HasItem[CancellationReason] = _.item()
  implicit val hasItemItemResponse: HasItem[ItemResponse]             = _.item()
  implicit val hasItemPut: HasItem[Put]                               = _.item()
  implicit val hasItemPutItemRequest: HasItem[PutItemRequest]         = _.item()
  implicit val hasItemPutRequest: HasItem[PutRequest]                 = _.item()
}
