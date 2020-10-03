package d4s.codecs.circe

import cats.implicits._
import d4s.codecs.D4SDecoder
import d4s.models.DynamoException.DecoderException
import io.circe.{Decoder, Json}
import software.amazon.awssdk.core.util.{DefaultSdkAutoConstructList, DefaultSdkAutoConstructMap}
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import scala.jdk.CollectionConverters._

object D4SCirceDecoder {

  def derived[T: Decoder]: D4SDecoder[T] = new D4SDecoder[T] {
    override def decodeObject(v: Map[String, AttributeValue]): Either[DecoderException, T] = decodeImpl(v, attributeMapToJson(v))
    override def decode(v: AttributeValue): Either[DecoderException, T]                    = decodeImpl(v, attributeToJson(v))
    @inline private[this] def decodeImpl(v: Any, maybeJson: Option[Json]): Either[DecoderException, T] = {
      maybeJson
        .toRight(DecoderException(s"Couldn't decode dynamo item=${v.toString} as Json. A case wasn't handled in DynamoDecoder.attributeToJson", None))
        .flatMap(json => json.as[T].left.map(cause => DecoderException(s"Circe error when decoding item=${v.toString} json=$json: ${cause.getMessage}", Some(cause))))
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
    v.asMap.flatMap(attributeMapToJson)
  }

  def attributeMapToJson(v: Map[String, AttributeValue]): Option[Json] = {
    v.toList.traverse(_.traverse(attributeToJson)).map(Json.fromFields(_))
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
