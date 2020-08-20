package d4s.models.query.responses

import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, QueryResponse, ScanResponse}

trait HasItems[A] {
  def items(a: A): java.util.List[java.util.Map[String, AttributeValue]]
}

object HasItems {
  @inline def apply[A: HasItems]: HasItems[A] = implicitly

  implicit val hasItemsQueryResponse: HasItems[QueryResponse] = _.items()
  implicit val hasItemsScanResponse: HasItems[ScanResponse]   = _.items()
}
