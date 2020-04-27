package d4s.codecs

import java.util.UUID

import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType

final case class DynamoKeyAttribute[T](attrType: ScalarAttributeType)

object DynamoKeyAttribute {
  def apply[T: DynamoKeyAttribute]: DynamoKeyAttribute[T] = implicitly

  implicit val stringAttribute: DynamoKeyAttribute[String] = new DynamoKeyAttribute[String](ScalarAttributeType.S)
  implicit val byteAttribute: DynamoKeyAttribute[Byte]     = new DynamoKeyAttribute[Byte](ScalarAttributeType.N)
  implicit val shortAttribute: DynamoKeyAttribute[Short]   = new DynamoKeyAttribute[Short](ScalarAttributeType.N)
  implicit val intAttribute: DynamoKeyAttribute[Int]       = new DynamoKeyAttribute[Int](ScalarAttributeType.N)
  implicit val longAttribute: DynamoKeyAttribute[Long]     = new DynamoKeyAttribute[Long](ScalarAttributeType.N)
  implicit val uuidAttribute: DynamoKeyAttribute[UUID]     = new DynamoKeyAttribute[UUID](ScalarAttributeType.S)

  implicit val SdkBytesAttribute: DynamoKeyAttribute[SdkBytes] = new DynamoKeyAttribute[SdkBytes](ScalarAttributeType.B)
}
