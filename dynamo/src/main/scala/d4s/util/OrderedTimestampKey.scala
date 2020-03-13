package d4s.util

import java.time.{LocalDateTime, ZonedDateTime}

import d4s.codecs.DynamoKeyAttribute
import izumi.fundamentals.platform.time.IzTime
import io.circe.{Decoder, Encoder}
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType

/**
  * A string representation of ZonedDateTime that preserves its native ordering
  * for use as a RangeKey in Dynamo
  * */
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
  implicit val encoder: Encoder[OrderedTimestampKey] = Encoder[String].contramap(_.asString)
  implicit val decoder: Decoder[OrderedTimestampKey] = Decoder[String].map(OrderedTimestampKey(_))

  implicit val keyAttribute: DynamoKeyAttribute[OrderedTimestampKey] = new DynamoKeyAttribute[OrderedTimestampKey](ScalarAttributeType.S)

  implicit val ordering: Ordering[OrderedTimestampKey] = Ordering[String].on(_.asString)
}
