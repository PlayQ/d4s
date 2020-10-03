package d4s.keys

import java.util.UUID

import d4s.codecs.{D4SAttributeEncoder, D4SDecoder, DynamoKeyAttribute}
import d4s.util.leftpadLongInvertNegative

/**
  * A string representation of UUID that preserves UUID and TimeUUID native ordering
  * for use as a RangeKey in Dynamo
  */
final case class OrderedUUIDKey private (asString: String) extends AnyVal

object OrderedUUIDKey {
  def apply(uuid: UUID): OrderedUUIDKey = {
    val mostSignificantString  = leftpadLongInvertNegative(uuid.getMostSignificantBits)
    val leastSignificantString = leftpadLongInvertNegative(uuid.getLeastSignificantBits)

    val uuidStr = s"$mostSignificantString:$leastSignificantString"

    new OrderedUUIDKey(uuidStr)
  }

  // encode as string
  implicit val encoder: D4SAttributeEncoder[OrderedUUIDKey] = D4SAttributeEncoder[String].contramap(_.asString)
  implicit val decoder: D4SDecoder[OrderedUUIDKey]          = D4SDecoder[String].map(OrderedUUIDKey(_))

  implicit val keyAttribute: DynamoKeyAttribute[OrderedUUIDKey] = DynamoKeyAttribute.S

  implicit val ordering: Ordering[OrderedUUIDKey] = Ordering[String].on(_.asString)
}
