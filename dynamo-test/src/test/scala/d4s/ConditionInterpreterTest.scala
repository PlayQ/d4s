package d4s

import d4s.models.conditions.Condition._
import d4s.models.conditions.LogicalOperator
import org.scalatest.wordspec.AnyWordSpec

class ConditionInterpreterTest extends AnyWordSpec {

  "produce correct unary expressions" in {
    val simpleBetweenExpr = "#a between :l_0 and :r_0"
    val simpleInExpr      = "#product in (:item0_0,:item0_1)"

    val betweenCondition = between("a", "b", "c")
    val evalResult       = betweenCondition.eval
    assert(evalResult.conditionExpression.get == simpleBetweenExpr)

    val inCondition  = in("product", Set("shop1", "shop2"))
    val inCondResult = inCondition.eval
    assert(inCondResult.conditionExpression.get == simpleInExpr)
  }

  "produce correct logic expressions" in {
    val number = 3
    val gt     = logical("a", LogicalOperator.>, number)
    val lt     = logical("a", LogicalOperator.<, number)
    val eq     = logical("a", LogicalOperator.==, number)

    assert(gt.eval.conditionExpression.get == "#a > :v_0")
    assert(lt.eval.conditionExpression.get == "#a < :v_0")
    assert(eq.eval.conditionExpression.get == "#a = :v_0")
  }

  "produce correct complex conditions" in {
    val expectedExpr1 = "(#a between :l_1 and :r_1 AND (#d < :v_3 OR #e <> :v_4))"
    val complexExpr1  = between("a", "b", "c") AND (logical("d", LogicalOperator.<, 4) OR logical("e", LogicalOperator.<>, 10))
    val testResult1   = complexExpr1.eval
    assert(testResult1.conditionExpression.get == expectedExpr1)

    val expectedExpr2 = "((#d < :v_2 AND #e <> :v_3) OR #product in (:item4_0,:item4_1))"
    val complexExpr2  = (logical("d", LogicalOperator.<, 4) AND logical("e", LogicalOperator.<>, 10)) OR in("product", Set("shop1", "shop2"))
    val testResult2   = complexExpr2.eval
    assert(testResult2.conditionExpression.get == expectedExpr2)
  }

}
