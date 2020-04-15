package d4s.codecs.circe

import cats.implicits._
import d4s.codecs.CodecsUtils.{CannotDecodeAttributeValueAsJson, CirceDecodeException, DynamoDecoderException}
import d4s.codecs.D4SDecoder
import io.circe.{Decoder, Json}
import software.amazon.awssdk.core.util.{DefaultSdkAutoConstructList, DefaultSdkAutoConstructMap}
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import scala.jdk.CollectionConverters._

object D4SCirceDecoder {

  def derived[T: Decoder]: D4SDecoder[T] = new D4SDecoder[T] {
    /** Not typesafe. This will only succeed if `T` is encoded as a JsonObject (has ObjectEncoder instance) */
    override def decode(item: Map[String, AttributeValue]): Either[DynamoDecoderException, T] = {
      for {
        json <- item.toList
          .traverse(_.traverse(attributeToJson)).map(Json.fromFields(_))
          .toRight(new CannotDecodeAttributeValueAsJson(item.toString))
        res <- json.as[T].left.map(new CirceDecodeException(item.toString, json, _))
      } yield res
    }

    /** This will succeed even if `T` is not encoded as a JsonObject */
    override def decodeAttribute(v: AttributeValue): Either[DynamoDecoderException, T] = {
      attributeToJson(v)
        .toRight(new CannotDecodeAttributeValueAsJson(v.toString))
        .flatMap(json => json.as[T].left.map(new CirceDecodeException(v.toString, json, _)))
    }
  }

  def attributeToJson(v: AttributeValue): Option[Json] = {
    v.asBool.map(Json.fromBoolean) orElse
    v.asString.map(Json.fromString) orElse
    v.asNumber.map(Json.fromString) orElse
    v.asNull.as(Json.Null) orElse
    v.asCollection.flatMap(v => v.traverse(attributeToJson).map(Json.fromValues)) orElse
    v.asStringsSet.map(v => Json.fromValues(v.map(Json.fromString))) orElse
    v.asNumberSet.map(v => Json.fromValues(v.map(Json.fromString))) orElse
    v.asMap.flatMap(v => v.toList.traverse(_.traverse(attributeToJson)).map(Json.fromFields(_)))
  }

  @SuppressWarnings(Array("IsInstanceOf"))
  implicit final class AttributeOps(private val v: AttributeValue) extends AnyVal {
    def asNumber: Option[String]                   = Option(v.n())
    def asBool: Option[Boolean]                    = Option(v.bool()).map(_.booleanValue())
    def asNull: Option[Unit]                       = Option(v.nul()).map(_.booleanValue()).collect { case true => () }
    def asString: Option[String]                   = Option(v.s())
    def asStringsSet: Option[Set[String]]          = Option(v.ss()).filter(!_.isInstanceOf[DefaultSdkAutoConstructList[_]]).map(_.asScala.toSet)
    def asNumberSet: Option[Set[String]]           = Option(v.ns()).filter(!_.isInstanceOf[DefaultSdkAutoConstructList[_]]).map(_.asScala.toSet)
    def asCollection: Option[List[AttributeValue]] = Option(v.l()).filter(!_.isInstanceOf[DefaultSdkAutoConstructList[_]]).map(_.asScala.toList)
    def asMap: Option[Map[String, AttributeValue]] = Option(v.m()).filter(!_.isInstanceOf[DefaultSdkAutoConstructMap[_, _]]).map(_.asScala.toMap)
  }
}
