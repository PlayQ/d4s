package d4s.models.table

import d4s.models.DynamoExecution
import d4s.models.query.DynamoRequest.WithTableReference
import d4s.models.query.{DynamoQuery, DynamoRequest}

import scala.language.implicitConversions

trait DynamoPrefixedSyntax {

  def ddl: TableDDL

  implicit final def queryPrefixed[DR <: DynamoRequest, Dec](query: DynamoQuery[DR, Dec])(implicit ev: DR <:< WithTableReference[DR]): Prefixed[DR, Dec, Dec] = {
    new Prefixed[DR, Dec, Dec](query)
  }

  implicit final class Prefixed[DR <: DynamoRequest, Dec, A](exec: DynamoExecution[DR, Dec, A])(implicit ev: DR <:< WithTableReference[DR]) {
    def prefixed[TP: TablePrefix](prefix: TP): DynamoExecution[DR, Dec, A] = {
      exec.modify(_.withPrefix(prefix)).retryWithPrefix(ddl)
    }
  }

}
