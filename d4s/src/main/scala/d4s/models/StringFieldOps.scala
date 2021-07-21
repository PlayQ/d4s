package d4s.models

import d4s.codecs.{D4SAttributeEncoder, DynamoKeyAttribute}
import d4s.models.StringFieldOps.{PathBasedFieldOpsCtor, StringTypedFieldOpsCtor}
import d4s.models.conditions._
import d4s.models.table.DynamoField

import scala.language.implicitConversions

trait StringFieldOps {
  @inline implicit final def stringToTypedFieldOps(s: String): StringTypedFieldOpsCtor = new StringTypedFieldOpsCtor(s)
  @inline implicit final def pathToFieldOps(path: List[String]): PathBasedFieldOpsCtor = new PathBasedFieldOpsCtor(path)
}

object StringFieldOps {

  final class StringTypedFieldOpsCtor(private val name: String) extends AnyVal {
    def of[T]: TypedFieldOps[T] = new TypedFieldOps[T](List(name))

    def field[T: DynamoKeyAttribute: D4SAttributeEncoder]: DynamoField[T] = DynamoField(name)

    def existsField: Condition.attribute_exists           = Condition.attribute_exists(List(name))
    def notExists: Condition.attribute_not_exists         = Condition.attribute_not_exists(List(name))
    def hasType(tpe: String): Condition.attribute_type    = Condition.attribute_type(List(name), tpe)
    def sizeField: Condition.size                         = Condition.size(List(name))
    def beginsWith(substr: String): Condition.begins_with = Condition.begins_with(List(name), substr)
  }

  final class PathBasedFieldOpsCtor(private val path: List[String]) extends AnyVal {
    def of[T]: TypedFieldOps[T] = new TypedFieldOps[T](path)

    def existsField: Condition.attribute_exists           = Condition.attribute_exists(path)
    def notExists: Condition.attribute_not_exists         = Condition.attribute_not_exists(path)
    def hasType(tpe: String): Condition.attribute_type    = Condition.attribute_type(path, tpe)
    def sizeField: Condition.size                         = Condition.size(path)
    def beginsWith(substr: String): Condition.begins_with = Condition.begins_with(path, substr)
  }

  final class TypedFieldOps[T](private val path: List[String]) extends AnyVal {
    def exists: Condition.attribute_exists                = Condition.attribute_exists(path)
    def notExists: Condition.attribute_not_exists         = Condition.attribute_not_exists(path)
    def hasType(tpe: String): Condition.attribute_type    = Condition.attribute_type(path, tpe)
    def size: Condition.size                              = Condition.size(path)
    def beginsWith(substr: String): Condition.begins_with = Condition.begins_with(path, substr)

    def contains(value: T)(implicit enc: D4SAttributeEncoder[T]): Condition.contains[T] = {
      Condition.contains(path, value)
    }

    def contains[In](value: In)(implicit enc: D4SAttributeEncoder[In], ev: T <:< Iterable[In]): Condition.contains[In] = {
      Condition.contains(path, value)
    }

    def isIn(set: Set[T])(implicit enc: D4SAttributeEncoder[T]): Condition.in[T] = {
      Condition.in(path, set)
    }

    def between(left: T, right: T)(implicit enc: D4SAttributeEncoder[T]): Condition.between[T] = {
      Condition.between(path, left, right)
    }

    def >(threshold: T)(implicit enc: D4SAttributeEncoder[T]): Condition.logical[T] = {
      Condition.logical(path, LogicalOperator.>, threshold)
    }

    def <(threshold: T)(implicit enc: D4SAttributeEncoder[T]): Condition.logical[T] = {
      Condition.logical(path, LogicalOperator.<, threshold)
    }

    def >=(threshold: T)(implicit enc: D4SAttributeEncoder[T]): Condition.logical[T] = {
      Condition.logical(path, LogicalOperator.>=, threshold)
    }

    def <=(threshold: T)(implicit enc: D4SAttributeEncoder[T]): Condition.logical[T] = {
      Condition.logical(path, LogicalOperator.<=, threshold)
    }

    @SuppressWarnings(Array("AvoidOperatorOverload"))
    def ===(threshold: T)(implicit enc: D4SAttributeEncoder[T]): Condition.logical[T] = {
      Condition.logical(path, LogicalOperator.==, threshold)
    }

    def <>(threshold: T)(implicit enc: D4SAttributeEncoder[T]): Condition.logical[T] = {
      Condition.logical(path, LogicalOperator.<>, threshold)
    }
  }

}
