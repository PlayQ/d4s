package d4s.models.query.requests

import d4s.models.conditions.Condition
import d4s.models.conditions.Condition._
import d4s.models.query.DynamoRequest
import d4s.models.query.DynamoRequest.{WithAttributeNames, WithAttributeValues, WithCondition, WithItem, WithReturnValue, WithTableReference, WithUpdateExpression}
import d4s.models.table.TableReference
import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, ReturnValue, UpdateItemRequest, UpdateItemResponse}

import scala.jdk.CollectionConverters._
import scala.util.chaining._

final case class UpdateItem(
  table: TableReference,
  attributeValues: Map[String, AttributeValue]      = Map.empty,
  attributeNames: Map[String, String]               = Map.empty,
  updateExpressionItem: Map[String, AttributeValue] = Map.empty,
  updateExpression: String                          = "",
  condition: Condition                              = ZeroCondition,
  returnValue: Option[ReturnValue]                  = None,
) extends DynamoRequest
  with WithTableReference[UpdateItem]
  with WithAttributeValues[UpdateItem]
  with WithAttributeNames[UpdateItem]
  with WithCondition[UpdateItem]
  with WithUpdateExpression[UpdateItem]
  with WithReturnValue[UpdateItem]
  with WithItem[UpdateItem] {

  override type Rq  = UpdateItemRequest
  override type Rsp = UpdateItemResponse

  override def withCondition(c: Condition): UpdateItem = copy(condition = c)

  override def withAttributeNames(an: Map[String, String] => Map[String, String]): UpdateItem = copy(attributeNames = an(attributeNames))

  override def withTableReference(t: TableReference => TableReference): UpdateItem = copy(table = t(table))

  override def withAttributeValues(f: Map[String, AttributeValue] => Map[String, AttributeValue]): UpdateItem = copy(attributeValues = f(attributeValues))

  override def withUpdateExpression(f: String => String): UpdateItem = copy(updateExpression = f(updateExpression))

  override def withItemAttributeValues(f: Map[String, AttributeValue] => Map[String, AttributeValue]): UpdateItem = copy(updateExpressionItem = f(updateExpressionItem))

  override def withReturnValue(newReturnValue: ReturnValue): UpdateItem = copy(returnValue = Some(newReturnValue))

  override def toAmz: UpdateItemRequest = {
    val (key, data)    = updateExpressionItem.partition { case (k, _) => table.key.keyNames.contains(k) }
    val setExprs       = data.keySet.map(i => s"#$i = :$i").toList
    val setExprsString = setExprs.mkString(", ")

    val updateExprAttributeValues = data.map { case (k, v) => s":$k" -> v }
    val updateExprAttributeNames  = data.keySet.map(i => s"#$i" -> i).toMap ++ attributeNames

    val attrValues = updateExprAttributeValues ++ attributeValues

    val updateExpr = if (setExprs.isEmpty) {
      updateExpression
    } else {
      updateExpression.split("SET ", 2).toList match {
        case List(beforeSET, "") =>
          beforeSET + s"SET $setExprsString"
        case List(beforeSET, afterSET) =>
          beforeSET + s"SET $setExprsString, $afterSET"
        case List(updateExpression) =>
          s"SET $setExprsString $updateExpression"
        case _ =>
          updateExpression
      }
    }
    val condExpr = condition.eval

    UpdateItemRequest
      .builder()
      .tableName(table.fullName)
      .updateExpression(updateExpr)
      .key(key.asJava)
      .conditionExpression(condExpr.conditionExpression.orNull)
      .expressionAttributeValues(condExpr.withAttributes(attrValues))
      .expressionAttributeNames(condExpr.withAliases(updateExprAttributeNames))
      .pipe(b => returnValue.fold(b)(b.returnValues))
      .build()
  }

}
