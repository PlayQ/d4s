package d4s.codecs

import java.util.UUID

import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType

trait DynamoKeyAttribute[T] {
  def attrType: ScalarAttributeType
}

object DynamoKeyAttribute {
  def apply[T: DynamoKeyAttribute]: DynamoKeyAttribute[T] = implicitly

  def apply[T](scalarAttributeType: ScalarAttributeType): DynamoKeyAttribute[T] = new DynamoKeyAttribute[T] {
    override def attrType: ScalarAttributeType = scalarAttributeType
  }

  def S[T]: DynamoKeyAttribute[T] = DynamoKeyAttribute(ScalarAttributeType.S)
  def N[T]: DynamoKeyAttribute[T] = DynamoKeyAttribute(ScalarAttributeType.N)
  def B[T]: DynamoKeyAttribute[T] = DynamoKeyAttribute(ScalarAttributeType.B)

  implicit val stringAttribute: DynamoKeyAttribute[String] = DynamoKeyAttribute.S
  implicit val uuidAttribute: DynamoKeyAttribute[UUID]     = DynamoKeyAttribute.S

  implicit val byteAttribute: DynamoKeyAttribute[Byte]   = DynamoKeyAttribute.N
  implicit val shortAttribute: DynamoKeyAttribute[Short] = DynamoKeyAttribute.N
  implicit val intAttribute: DynamoKeyAttribute[Int]     = DynamoKeyAttribute.N
  implicit val longAttribute: DynamoKeyAttribute[Long]   = DynamoKeyAttribute.N

  implicit val sdkBytesAttribute: DynamoKeyAttribute[SdkBytes]     = DynamoKeyAttribute.B
  implicit val arrayByteAttribute: DynamoKeyAttribute[Array[Byte]] = DynamoKeyAttribute.B
}
