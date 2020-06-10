package d4s.models.query.requests

import d4s.models.conditions.Condition
import d4s.models.conditions.Condition._
import d4s.models.query.DynamoRequest
import d4s.models.query.DynamoRequest.{WithAttributeNames, WithAttributeValues, WithCondition, WithKey, WithTableReference}
import d4s.models.table.TableReference
import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, DeleteItemRequest, DeleteItemResponse}

import scala.jdk.CollectionConverters._

final case class DeleteItem(
  table: TableReference,
  conditionExpression: Condition               = ZeroCondition,
  attributeValues: Map[String, AttributeValue] = Map.empty,
  attributeNames: Map[String, String]          = Map.empty,
  key: Map[String, AttributeValue]             = Map.empty,
) extends DynamoRequest
  with WithAttributeValues[DeleteItem]
  with WithTableReference[DeleteItem]
  with WithCondition[DeleteItem]
  with WithKey[DeleteItem]
  with WithAttributeNames[DeleteItem] {

  override type Rq  = DeleteItemRequest
  override type Rsp = DeleteItemResponse

  override def withKey(f: Map[String, AttributeValue] => Map[String, AttributeValue]): DeleteItem = copy(key = f(key))

  override def withCondition(c: Condition): DeleteItem = copy(conditionExpression = conditionExpression && c)

  override def withAttributeNames(an: Map[String, String] => Map[String, String]): DeleteItem = copy(attributeNames = an(attributeNames))

  override def withTableReference(t: TableReference => TableReference): DeleteItem = copy(table = t(table))

  override def withAttributeValues(f: Map[String, AttributeValue] => Map[String, AttributeValue]): DeleteItem = copy(attributeValues = f(attributeValues))

  override def toAmz: DeleteItemRequest = {
    val evaluatedCondition = conditionExpression.eval

    DeleteItemRequest
      .builder()
      .tableName(table.fullName)
      .key(key.asJava)
      .conditionExpression(evaluatedCondition.conditionExpression.orNull)
      .expressionAttributeValues(evaluatedCondition.withAttributes(attributeValues))
      .expressionAttributeNames(evaluatedCondition.withAliases(attributeNames))
      .build()
  }
}
