package d4s.util

import java.time.ZonedDateTime

import d4s.codecs.{D4SAttributeEncoder, D4SDecoder, DynamoKeyAttribute}
import izumi.fundamentals.platform.language.Quirks
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType

/** A [[OrderedTimestampKey]] rendered with inverted digits to reverse range key ordering in DynamoDB */
final case class ReversedTimestampKey private (asString: String) extends AnyVal

object ReversedTimestampKey {
  def apply(tsz: ZonedDateTime): ReversedTimestampKey = {
    apply(OrderedTimestampKey(tsz))
  }

  def apply(digitTimestamp: OrderedTimestampKey)(implicit dummy: DummyImplicit): ReversedTimestampKey = {
    Quirks.discard(dummy)
    ReversedTimestampKey(negateDigits(digitTimestamp.asString))
  }

  // encode as string
  implicit val encoder: D4SAttributeEncoder[ReversedTimestampKey] = D4SAttributeEncoder[String].contramap(_.asString)
  implicit val decoder: D4SDecoder[ReversedTimestampKey]          = D4SDecoder[String].map(ReversedTimestampKey(_))

  implicit val keyAttribute: DynamoKeyAttribute[ReversedTimestampKey] = new DynamoKeyAttribute[ReversedTimestampKey](ScalarAttributeType.S)

  implicit val ordering: Ordering[ReversedTimestampKey] = Ordering[String].on(_.asString)
}
