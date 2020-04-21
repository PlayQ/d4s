package d4s.util

import java.util.UUID

import d4s.codecs.{D4SAttributeEncoder, D4SDecoder, DynamoKeyAttribute}
import izumi.fundamentals.platform.language.Quirks
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType

/** An [[OrderedUUIDKey]] rendered with inverted digits and minus sign to reverse range key ordering in DynamoDB */
final case class ReversedUUIDKey private (asString: String) extends AnyVal

object ReversedUUIDKey {
  def apply(uuid: UUID): ReversedUUIDKey = {
    apply(OrderedUUIDKey(uuid))
  }

  def apply(digitUUID: OrderedUUIDKey)(implicit dummy: DummyImplicit): ReversedUUIDKey = {
    Quirks.discard(dummy)
    ReversedUUIDKey(negateMinus(negateDigits(digitUUID.asString)))
  }

  // encode as string
  implicit val encoder: D4SAttributeEncoder[ReversedUUIDKey] = D4SAttributeEncoder[String].contramap(_.asString)
  implicit val decoder: D4SDecoder[ReversedUUIDKey]          = D4SDecoder[String].map(ReversedUUIDKey(_))

  implicit val keyAttribute: DynamoKeyAttribute[ReversedUUIDKey] = new DynamoKeyAttribute[ReversedUUIDKey](ScalarAttributeType.S)

  implicit val ordering: Ordering[ReversedUUIDKey] = Ordering[String].on(_.asString)
}
