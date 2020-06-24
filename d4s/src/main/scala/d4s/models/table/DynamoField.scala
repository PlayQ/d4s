package d4s.models.table

import d4s.codecs.{D4SAttributeEncoder, DynamoKeyAttribute}
import d4s.models.StringFieldOps.TypedFieldOps
import software.amazon.awssdk.services.dynamodb.model.{AttributeDefinition, AttributeValue, ScalarAttributeType}

import scala.language.implicitConversions

final case class DynamoField[-T](name: String, attrType: ScalarAttributeType, encoder: T => AttributeValue) {
  override def toString: String = name

  def toAttribute: AttributeDefinition = {
    AttributeDefinition
      .builder()
      .attributeName(name)
      .attributeType(attrType)
      .build()
  }

  def bind(value: T): (String, AttributeValue) = name -> encoder(value)

  def contramap[B](f: B => T): DynamoField[B] = copy(encoder = encoder apply f(_))
}

object DynamoField {
  def apply[T](name: String)(implicit fieldAttribute: DynamoKeyAttribute[T], encoder: D4SAttributeEncoder[T]): DynamoField[T] = {
    DynamoField(name, fieldAttribute.attrType, encoder.encode)
  }

  @inline implicit final def fieldToTypedFieldOps[T](field: DynamoField[T]): TypedFieldOps[T] = new TypedFieldOps[T](List(field.name))
}
