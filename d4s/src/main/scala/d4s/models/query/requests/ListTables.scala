package d4s.models.query.requests

import d4s.models.query.DynamoRequest
import d4s.models.query.DynamoRequest.PageableRequest
import software.amazon.awssdk.services.dynamodb.model.{ListTablesRequest, ListTablesResponse}

import scala.util.chaining._

final case class ListTables(
  startTable: Option[String] = None
) extends DynamoRequest {

  override type Rq  = ListTablesRequest
  override type Rsp = ListTablesResponse

  def withStartTable(startTable: String): ListTables = copy(startTable = Some(startTable))

  override def toAmz: ListTablesRequest = {
    ListTablesRequest
      .builder()
      .pipe(b => startTable.fold(b)(b.exclusiveStartTableName))
      .build()
  }
}
object ListTables {
  implicit val pageableRequest: PageableRequest[ListTables] = PageableRequest[ListTables](rsp => Option(rsp.lastEvaluatedTableName()))(_.withStartTable(_))
}
