package d4s.codecs

import java.util.UUID

import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType

final case class DynamoKeyAttribute[T](attrType: ScalarAttributeType)

object DynamoKeyAttribute {
  def apply[T: DynamoKeyAttribute]: DynamoKeyAttribute[T] = implicitly

  implicit val StringAttribute: DynamoKeyAttribute[String] = new DynamoKeyAttribute[String](ScalarAttributeType.S)
  implicit val IntAttribute: DynamoKeyAttribute[Int]       = new DynamoKeyAttribute[Int](ScalarAttributeType.N)
  implicit val LongAttribute: DynamoKeyAttribute[Long]     = new DynamoKeyAttribute[Long](ScalarAttributeType.N)
  implicit val UUIDAttribute: DynamoKeyAttribute[UUID]     = new DynamoKeyAttribute[UUID](ScalarAttributeType.S)

  implicit val SdkBytesAttribute: DynamoKeyAttribute[SdkBytes] = new DynamoKeyAttribute[SdkBytes](ScalarAttributeType.B)
}
