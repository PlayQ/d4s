package d4s.models.conditions

import d4s.codecs
import d4s.codecs.D4SAttributeEncoder
import d4s.models.conditions.Condition.{FinalCondition, and, not, or}
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import scala.jdk.CollectionConverters._

trait Condition {
  final def AND(that: Condition): Condition.and = and(this, that)
  final def OR(that: Condition): Condition.or   = or(this, that)

  final def &&(that: Condition): Condition.and = AND(that)
  final def ||(that: Condition): Condition.or  = OR(that)

  final def unary_! : Condition.not = not(this)

  final def eval: FinalCondition = evalRecursive(0)._2

  protected def evalRecursive(nesting: Int): (Int, FinalCondition)
}

object Condition {
  private[this] def createAlias(field: String): (String, Map[String, String]) = createAlias(List(field))

  private[this] def createAlias(path: List[String]): (String, Map[String, String]) = {
    val aliasToPart = path.map(s => s"#${s.replaceAll("\\.", "")}" -> s).toMap
    val fullPath    = aliasToPart.keys.mkString(".")
    fullPath -> aliasToPart
  }

  final case class FinalCondition(attrValues: Map[String, AttributeValue], aliases: Map[String, String], conditionExpression: Option[String]) {
    def withAttributes(other: Map[String, AttributeValue]): java.util.Map[String, AttributeValue] =
      mapOrNull(attrValues ++ other)

    def withAliases(other: Map[String, String]): java.util.Map[String, String] =
      mapOrNull(aliases ++ other)

    private[this] def mapOrNull[T, T1](m: Map[T, T1]): java.util.Map[T, T1] =
      if (m.isEmpty) null else m.asJava
  }

  trait Direct extends Condition {
    protected def eval(nesting: Int): FinalCondition

    override protected final def evalRecursive(nesting: Int): (Int, FinalCondition) = {
      (nesting, eval(nesting))
    }
  }

  final case class and(left: Condition, right: Condition) extends Condition {
    override protected def evalRecursive(nesting: Int): (Int, FinalCondition) =
      evalBinary("AND")(nesting, left, right)
  }

  final case class or(left: Condition, right: Condition) extends Condition {
    override protected def evalRecursive(nesting: Int): (Int, FinalCondition) =
      evalBinary("OR")(nesting, left, right)
  }

  final case class not(cond: Condition) extends Condition {
    override protected def evalRecursive(nesting: Int): (Int, FinalCondition) = {
      val (updatedCounter, result) = cond.evalRecursive(nesting + 1)
      val condExpr                 = result.conditionExpression.map(s => s"NOT $s")
      updatedCounter -> FinalCondition(result.attrValues, result.aliases, condExpr)
    }
  }

  final case class between[T: codecs.D4SAttributeEncoder](field: String, left: T, right: T) extends Condition.Direct {
    override protected def eval(nesting: Int): FinalCondition = {
      val leftId       = s":l_$nesting"
      val rightId      = s":r_$nesting"
      val (alias, map) = createAlias(field)
      val attrValues: Map[String, AttributeValue] = {
        D4SAttributeEncoder.encodePlain(leftId, left) ++
        D4SAttributeEncoder.encodePlain(rightId, right)
      }
      val condExpr = s"$alias between $leftId and $rightId"
      FinalCondition(attrValues, map, Option(condExpr))
    }
  }

  final case class in[T: codecs.D4SAttributeEncoder](field: String, items: Set[T]) extends Condition.Direct {
    override protected def eval(nesting: Int): FinalCondition = {
      val attrValues: Map[String, AttributeValue] = items.zipWithIndex.foldLeft(Map.empty[String, AttributeValue]) {
        case (acc, (item, id)) =>
          acc ++ D4SAttributeEncoder.encodePlain(s":item${nesting}_$id", item)
      }
      val (alias, map) = createAlias(field)
      val condExpr     = s"$alias in (${attrValues.keySet.mkString(",")})"
      FinalCondition(attrValues, map, Option(condExpr))
    }
  }

  final case class logical[T: codecs.D4SAttributeEncoder](field: String, operator: LogicalOperator, value: T) extends Condition.Direct {
    override protected def eval(nesting: Int): FinalCondition = {
      val valName      = s":v_$nesting"
      val attrValues   = D4SAttributeEncoder.encodePlain(valName, value)
      val (alias, map) = createAlias(field)
      val condExpr     = s"$alias ${operator.asString} $valName"
      FinalCondition(attrValues, map, Some(condExpr))
    }
  }

  final case class begins_with(path: List[String], prefix: String) extends Condition.Direct {
    override protected def eval(nesting: Int): FinalCondition = {
      val (alias, map) = createAlias(path)
      val valName      = s":vb_$nesting"
      val attrValues   = D4SAttributeEncoder.encodePlain(valName, prefix)
      val condExpr     = s"begins_with($alias, $valName)"
      FinalCondition(attrValues, map, Some(condExpr))
    }
  }

  final case class attribute_exists(path: List[String], pathBased: Boolean = false) extends Condition.Direct {
    override protected def eval(nesting: Int): FinalCondition = {
      val (alias, map) = createAlias(path)
      val condExpr     = s"attribute_exists($alias)"
      FinalCondition(Map.empty, map, Some(condExpr))
    }
  }

  final case class attribute_not_exists(path: List[String], pathBased: Boolean = false) extends Condition.Direct {
    override protected def eval(nesting: Int): FinalCondition = {
      val (alias, map) = createAlias(path)
      val condExpr     = s"attribute_not_exists($alias)"
      FinalCondition(Map.empty, map, Some(condExpr))
    }
  }

  final case class raw(conditionString: String) extends Condition.Direct {
    override protected def eval(nesting: Int): FinalCondition =
      FinalCondition(Map.empty, Map.empty, Some(conditionString))
  }

  case object ZeroCondition extends Condition.Direct {
    private[this] val empty                                   = FinalCondition(Map.empty, Map.empty, None)
    override protected def eval(nesting: Int): FinalCondition = empty
  }

  private[this] def evalBinary(operation: String)(nesting: Int, left: Condition, right: Condition): (Int, FinalCondition) = {
    val (updatedCounter, leftRes) = left.evalRecursive(nesting + 1)
    val (finalCounter, rightRes)  = right.evalRecursive(updatedCounter + 1)
    (finalCounter, combineWith(leftRes, rightRes, operation))
  }

  private[this] def combineWith(cond: FinalCondition, cond2: FinalCondition, operation: String): FinalCondition = {
    val condExpr = (cond.conditionExpression, cond2.conditionExpression) match {
      case (Some(l), Some(r))      => Some(s"($l $operation $r)")
      case (None, value @ Some(_)) => value
      case (value @ Some(_), None) => value
      case (None, None)            => None
    }

    val attrValues   = cond.attrValues ++ cond2.attrValues
    val fieldAliases = cond.aliases ++ cond2.aliases
    FinalCondition(attrValues, fieldAliases, condExpr)
  }

}
