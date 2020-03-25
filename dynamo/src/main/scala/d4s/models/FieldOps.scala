package d4s.models

import d4s.codecs.{D4SAttributeEncoder, DynamoKeyAttribute}
import d4s.models.FieldOps.{PathBasedFieldOpsCtor, StringTypedFieldOpsCtor}
import d4s.models.conditions._
import d4s.models.table.DynamoField

import scala.language.implicitConversions

trait FieldOps {
  @inline implicit final def stringToTypedFieldOps(s: String): StringTypedFieldOpsCtor = new StringTypedFieldOpsCtor(s)
  @inline implicit final def pathToFieldOps(path: List[String]): PathBasedFieldOpsCtor = new PathBasedFieldOpsCtor(path)
}

object FieldOps {

  final class PathBasedFieldOpsCtor(private val path: List[String]) extends AnyVal {
    def existsField: Condition.attribute_exists           = Condition.attribute_exists(path)
    def notExists: Condition.attribute_not_exists         = Condition.attribute_not_exists(path)
    def beginsWith(substr: String): Condition.begins_with = Condition.begins_with(path, substr)
  }

  final class StringTypedFieldOpsCtor(private val name: String) extends AnyVal {
    def of[T]: TypedFieldOps[T] = new TypedFieldOps[T](name)

    def field[T: DynamoKeyAttribute: D4SAttributeEncoder]: DynamoField[T] = DynamoField(name)

    def existsField: Condition.attribute_exists           = Condition.attribute_exists(List(name))
    def notExists: Condition.attribute_not_exists         = Condition.attribute_not_exists(List(name))
    def beginsWith(substr: String): Condition.begins_with = Condition.begins_with(List(name), substr)
  }

  final class TypedFieldOps[T](private val name: String) extends AnyVal {
    def exists: Condition.attribute_exists                = Condition.attribute_exists(List(name))
    def notExists: Condition.attribute_not_exists         = Condition.attribute_not_exists(List(name))
    def beginsWith(substr: String): Condition.begins_with = Condition.begins_with(List(name), substr)

    def isIn(set: Set[T])(implicit enc: D4SAttributeEncoder[T]): Condition.in[T] = {
      Condition.in(name, set)
    }

    def between(left: T, right: T)(implicit enc: D4SAttributeEncoder[T]): Condition.between[T] = {
      Condition.between(name, left, right)
    }

    def >(threshold: T)(implicit enc: D4SAttributeEncoder[T]): Condition.logical[T] = {
      Condition.logical(name, LogicalOperator.>, threshold)
    }

    def <(threshold: T)(implicit enc: D4SAttributeEncoder[T]): Condition.logical[T] = {
      Condition.logical(name, LogicalOperator.<, threshold)
    }

    def >=(threshold: T)(implicit enc: D4SAttributeEncoder[T]): Condition.logical[T] = {
      Condition.logical(name, LogicalOperator.>=, threshold)
    }

    def <=(threshold: T)(implicit enc: D4SAttributeEncoder[T]): Condition.logical[T] = {
      Condition.logical(name, LogicalOperator.<=, threshold)
    }

    @SuppressWarnings(Array("AvoidOperatorOverload"))
    def ===(threshold: T)(implicit enc: D4SAttributeEncoder[T]): Condition.logical[T] = {
      Condition.logical(name, LogicalOperator.==, threshold)
    }

    def <>(threshold: T)(implicit enc: D4SAttributeEncoder[T]): Condition.logical[T] = {
      Condition.logical(name, LogicalOperator.<>, threshold)
    }
  }

}
