package d4s.models.table

import java.util

import d4s.codecs.{D4SAttributeEncoder, DynamoKeyAttribute}
import fs2.INothing
import software.amazon.awssdk.services.dynamodb.model.{AttributeDefinition, AttributeValue, KeySchemaElement, KeyType}

import scala.jdk.CollectionConverters._

final case class DynamoKey[-H, -R](
  hashKey: DynamoField[H],
  rangeKey: Option[DynamoField[R]],
) {
  def toList: List[KeySchemaElement] = {
    element(hashKey.name, KeyType.HASH) :: rangeKey.map(r => element(r.name, KeyType.RANGE)).toList
  }
  def toJava: util.List[KeySchemaElement]     = toList.asJava
  def toAttributes: List[AttributeDefinition] = (hashKey :: rangeKey.toList).map(_.toAttribute)

  def keyFields: Set[DynamoField[_]] = rangeKey.toSet[DynamoField[_]] + hashKey
  def keyNames: Set[String]          = rangeKey.map(_.name).toSet + hashKey.name

  def bind(hashValue: H, rangeValue: Option[R] = None): Map[String, AttributeValue] = {
    val mbRange = for {
      k <- rangeKey
      v <- rangeValue
    } yield Map(k.bind(v))
    Map(hashKey.bind(hashValue)) ++ mbRange.getOrElse(Map.empty)
  }
  def bind(hashValue: H, rangeValue: R): Map[String, AttributeValue] = bind(hashValue, Some(rangeValue))
  def bind(hashValue: H): Map[String, AttributeValue]                = bind(hashValue, None)

  def contramapHash[H1](f: H1 => H): DynamoKey[H1, R]                  = copy(hashKey = hashKey.contramap(f))
  def contramapRange[R1](f: R1 => R): DynamoKey[H, R1]                 = copy(rangeKey = rangeKey.map(_.contramap(f)))
  def contramapBoth[H1, R1](f: H1 => H, g: R1 => R): DynamoKey[H1, R1] = copy(hashKey = hashKey.contramap(f), rangeKey = rangeKey.map(_.contramap(g)))

  private[this] def element(name: String, tpe: KeyType): KeySchemaElement = {
    KeySchemaElement.builder().attributeName(name).keyType(tpe).build()
  }
}

object DynamoKey {
  def apply[H](hashKey: DynamoField[H]): DynamoKey[H, INothing]                       = DynamoKey(hashKey, None)
  def apply[H, R](hashKey: DynamoField[H], rangeKey: DynamoField[R]): DynamoKey[H, R] = DynamoKey(hashKey, Some(rangeKey))

  def apply[H](hashKey: String)(implicit ev0: DynamoKeyAttribute[H], ev1: D4SAttributeEncoder[H]): DynamoKey[H, INothing] =
    DynamoKey(DynamoField[H](hashKey), None)
  def apply[H, R](
    hashKey: String,
    rangeKey: String,
  )(implicit
    ev0: DynamoKeyAttribute[H],
    ev1: D4SAttributeEncoder[H],
    ev2: DynamoKeyAttribute[R],
    ev3: D4SAttributeEncoder[R],
  ): DynamoKey[H, R] =
    DynamoKey(DynamoField[H](hashKey), Some(DynamoField[R](rangeKey)))
}
