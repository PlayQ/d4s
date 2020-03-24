package d4s.codecs.circe

import java.util

import d4s.codecs.circe.DynamoAttributeEncoder.jsonToAttribute
import io.circe.Encoder
import io.circe.syntax._
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import scala.jdk.CollectionConverters._

trait DynamoEncoder[T] extends DynamoAttributeEncoder[T] {
  def encode(item: T): Map[String, AttributeValue]

  def encodeJava(item: T): java.util.Map[String, AttributeValue] = {
    encode(item).asJava
  }

  final def encodeAttribute(item: T): AttributeValue = {
    AttributeValue.builder().m(encodeJava(item)).build()
  }
}

object DynamoEncoder {
  def apply[T: DynamoEncoder]: DynamoEncoder[T] = implicitly

  def encode[A: DynamoEncoder](a: A): Map[String, AttributeValue]               = DynamoEncoder[A].encode(a)
  def encodeJava[A: DynamoEncoder](a: A): java.util.Map[String, AttributeValue] = DynamoEncoder[A].encodeJava(a)

  implicit val attributeMapEncoder: DynamoEncoder[Map[String, AttributeValue]] = a => a
  implicit val javaAttributeMapEncoder: DynamoEncoder[java.util.Map[String, AttributeValue]] = {
    new DynamoEncoder[util.Map[String, AttributeValue]] {
      override final def encode(item: java.util.Map[String, AttributeValue]): Map[String, AttributeValue] =
        item.asScala.toMap
      override final def encodeJava(item: java.util.Map[String, AttributeValue]): util.Map[String, AttributeValue] =
        item
    }
  }
  /** ObjectEncoder == DynamoEncoder */
  implicit def fromJsonObjectEncoder[T: Encoder.AsObject]: DynamoEncoder[T] = {
    _.asJsonObject.toMap.map { case (k, v) => k -> jsonToAttribute(v) }
  }
}
