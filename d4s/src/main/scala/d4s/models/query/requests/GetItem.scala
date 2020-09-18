package d4s.models.query.requests

import d4s.models.query.DynamoRequest
import d4s.models.query.DynamoRequest.{WithAttributeNames, WithConsistent, WithKey, WithProjectionExpression, WithTableReference}
import d4s.models.table.TableReference
import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, GetItemRequest, GetItemResponse}

import scala.jdk.CollectionConverters._
import scala.util.chaining._

final case class GetItem(
  table: TableReference,
  projectionExpression: Option[String]         = None,
  attributeValues: Map[String, AttributeValue] = Map.empty,
  attributeNames: Map[String, String]          = Map.empty,
  key: Map[String, AttributeValue]             = Map.empty,
  consistent: Boolean                          = false,
) extends DynamoRequest
  with WithAttributeNames[GetItem]
  with WithProjectionExpression[GetItem]
  with WithTableReference[GetItem]
  with WithKey[GetItem]
  with WithConsistent[GetItem] {

  override type Rq  = GetItemRequest
  override type Rsp = GetItemResponse

  override def withKey(f: Map[String, AttributeValue] => Map[String, AttributeValue]): GetItem = copy(key = f(key))

  override def withAttributeNames(an: Map[String, String] => Map[String, String]): GetItem = copy(attributeNames = an(attributeNames))

  override def withTableReference(t: TableReference => TableReference): GetItem = copy(table = t(table))

  override def withProjectionExpression(f: Option[String] => Option[String]): GetItem = copy(projectionExpression = f(projectionExpression))

  override def withConsistent(consistentRead: Boolean): GetItem = copy(consistent = consistentRead)

  override def toAmz: GetItemRequest =
    GetItemRequest
      .builder()
      .tableName(table.fullName)
      .key(key.asJava)
      .expressionAttributeNames(if (attributeNames.isEmpty) null else attributeNames.asJava)
      .pipe(b => projectionExpression.fold(b)(b.projectionExpression))
      .consistentRead(consistent)
      .build()
}
