package d4s.models.query.responses

import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, DeleteItemResponse, PutItemResponse, UpdateItemResponse}

trait HasAttributes[A] {
  def attributes(a: A): java.util.Map[String, AttributeValue]
}

object HasAttributes {
  @inline def apply[A: HasAttributes]: HasAttributes[A] = implicitly

  implicit val hasAttributesDeleteItemResponse: HasAttributes[DeleteItemResponse] = _.attributes()
  implicit val hasAttributesPutItemResponse: HasAttributes[PutItemResponse]       = _.attributes()
  implicit val hasAttributesUpdateItemResponse: HasAttributes[UpdateItemResponse] = _.attributes()
}
