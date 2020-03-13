package d4s.util

import java.util.UUID

import d4s.codecs.DynamoKeyAttribute
import io.circe.{Decoder, Encoder}
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType

/**
  * A string representation of UUID that preserves UUID and TimeUUID native ordering
  * for use as a RangeKey in Dynamo
  * */
final case class OrderedUUIDKey private (asString: String) extends AnyVal

object OrderedUUIDKey {
  def apply(uuid: UUID): OrderedUUIDKey = {
    val mostSignificantString  = leftpadLongInvertNegative(uuid.getMostSignificantBits)
    val leastSignificantString = leftpadLongInvertNegative(uuid.getLeastSignificantBits)

    val uuidStr = s"$mostSignificantString:$leastSignificantString"

    new OrderedUUIDKey(uuidStr)
  }

  // encode as string
  implicit val encoder: Encoder[OrderedUUIDKey] = Encoder[String].contramap(_.asString)
  implicit val decoder: Decoder[OrderedUUIDKey] = Decoder[String].map(OrderedUUIDKey(_))

  implicit val keyAttribute: DynamoKeyAttribute[OrderedUUIDKey] = new DynamoKeyAttribute[OrderedUUIDKey](ScalarAttributeType.S)

  implicit val ordering: Ordering[OrderedUUIDKey] = Ordering[String].on(_.asString)
}
