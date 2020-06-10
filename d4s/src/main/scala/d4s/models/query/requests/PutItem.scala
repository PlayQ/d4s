package d4s.models.query.requests

import d4s.models.conditions.Condition
import d4s.models.conditions.Condition._
import d4s.models.query.DynamoRequest
import d4s.models.query.DynamoRequest.{WithAttributeNames, WithAttributeValues, WithCondition, WithItem, WithTableReference}
import d4s.models.table.TableReference
import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, PutItemRequest, PutItemResponse}

import scala.jdk.CollectionConverters._

final case class PutItem(
  table: TableReference,
  conditionExpression: Condition               = ZeroCondition,
  attributeValues: Map[String, AttributeValue] = Map.empty,
  attributeNames: Map[String, String]          = Map.empty,
  item: Map[String, AttributeValue]            = Map.empty,
) extends DynamoRequest
  with WithAttributeValues[PutItem]
  with WithAttributeNames[PutItem]
  with WithTableReference[PutItem]
  with WithCondition[PutItem]
  with WithItem[PutItem] {

  override type Rq  = PutItemRequest
  override type Rsp = PutItemResponse

  override def withItemAttributeValues(f: Map[String, AttributeValue] => Map[String, AttributeValue]): PutItem = copy(item = f(item))

  override def withCondition(c: Condition): PutItem = copy(conditionExpression = conditionExpression && c)

  override def withAttributeNames(an: Map[String, String] => Map[String, String]): PutItem = copy(attributeNames = an(attributeNames))

  override def withTableReference(t: TableReference => TableReference): PutItem = copy(table = t(table))

  override def withAttributeValues(f: Map[String, AttributeValue] => Map[String, AttributeValue]): PutItem = copy(attributeValues = f(attributeValues))

  override def toAmz: PutItemRequest = {
    val result = conditionExpression.eval

    PutItemRequest
      .builder()
      .tableName(table.fullName)
      .item(item.asJava)
      .conditionExpression(result.conditionExpression.orNull)
      .expressionAttributeValues(result withAttributes attributeValues)
      .expressionAttributeNames(result withAliases attributeNames)
      .build()
  }
}
