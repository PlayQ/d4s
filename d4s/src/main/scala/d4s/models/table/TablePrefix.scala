package d4s.models.table

import java.util.UUID

import d4s.models.table.TablePrefix.NamedPrefix

trait TablePrefix[A] {
  def asTablePrefix(a: A): NamedPrefix
}

object TablePrefix {
  def apply[TP: TablePrefix]: TablePrefix[TP] = implicitly

  final case class NamedPrefix(tagName: String, prefix: String) {
    def toTag: Map[String, String] = {
      Map(tagName -> prefix).filter {
        case (k, v) => k.nonEmpty && v.nonEmpty
      }
    }
  }

  implicit val uuidPrefix: TablePrefix[UUID]       = t => NamedPrefix("partition_uuid", t.toString)
  implicit val stringIdPrefix: TablePrefix[String] = s => NamedPrefix("partition_str", s)
}
