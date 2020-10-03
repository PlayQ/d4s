package d4s.keys

import java.time.{LocalDateTime, ZonedDateTime}

import d4s.codecs.{D4SAttributeEncoder, D4SDecoder, DynamoKeyAttribute}
import izumi.fundamentals.platform.time.IzTime

/**
  * A string representation of ZonedDateTime that preserves its native ordering
  * for use as a RangeKey in Dynamo
  */
final case class OrderedTimestampKey private (asString: String) extends AnyVal

object OrderedTimestampKey {
  def apply(tsz: ZonedDateTime): OrderedTimestampKey = {
    apply(tsz.withZoneSameInstant(IzTime.TZ_UTC).toLocalDateTime)
  }

  def apply(utcDateTime: LocalDateTime): OrderedTimestampKey = {
    val format = IzTime.ISO_LOCAL_DATE_TIME_3NANO

    OrderedTimestampKey(format.format(utcDateTime).filter(_.isDigit))
  }

  // encode as string
  implicit val encoder: D4SAttributeEncoder[OrderedTimestampKey] = D4SAttributeEncoder[String].contramap(_.asString)
  implicit val decoder: D4SDecoder[OrderedTimestampKey]          = D4SDecoder[String].map(OrderedTimestampKey(_))

  implicit val keyAttribute: DynamoKeyAttribute[OrderedTimestampKey] = DynamoKeyAttribute.S

  implicit val ordering: Ordering[OrderedTimestampKey] = Ordering[String].on(_.asString)
}
