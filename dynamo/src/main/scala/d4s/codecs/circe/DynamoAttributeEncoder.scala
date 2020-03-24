package d4s.codecs.circe

import io.circe.syntax._
import io.circe.{Encoder, Json}
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import scala.jdk.CollectionConverters._

trait DynamoAttributeEncoder[T] {
  def encodeAttribute(item: T): AttributeValue
}

object DynamoAttributeEncoder {
  def apply[T: DynamoAttributeEncoder]: DynamoAttributeEncoder[T] = implicitly

  def encodeAttribute[A: DynamoAttributeEncoder](a: A): AttributeValue = {
    DynamoAttributeEncoder[A].encodeAttribute(a)
  }
  def encodePlain[T: DynamoAttributeEncoder](name: String, item: T): Map[String, AttributeValue] = {
    Map(name -> DynamoAttributeEncoder[T].encodeAttribute(item))
  }

  implicit val fromAttributeValue: DynamoAttributeEncoder[AttributeValue] = a => a
  implicit val fromSdkBytes: DynamoAttributeEncoder[SdkBytes]             = AttributeValue.builder().b(_).build()
  implicit def fromJsonEncoder[T: Encoder]: DynamoAttributeEncoder[T]     = a => jsonToAttribute(a.asJson)

  def jsonToAttribute(json: Json): AttributeValue = {
    json.fold(
      jsonNull    = AttributeValue.builder().nul(true).build(),
      jsonBoolean = b => toAttr(b: java.lang.Boolean, _.bool),
      jsonNumber  = n => toAttr(n.toString, _.n),
      jsonString  = s => toAttr(s, _.s),
      jsonArray   = array => AttributeValue.builder.l(array.toList.map(jsonToAttribute).asJava).build(),
      jsonObject  = obj => toAttr(obj.toMap.map { case (k, v) => k -> jsonToAttribute(v) }.asJava, _.m)
    )
  }

  private[this] def toAttr[Z](t: Z, f: AttributeValue.Builder => Z => AttributeValue.Builder): AttributeValue = {
    f(AttributeValue.builder())(t).build()
  }
}
