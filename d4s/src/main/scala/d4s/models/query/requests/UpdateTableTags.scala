package d4s.models.query.requests

import d4s.models.query.DynamoRequest
import d4s.models.query.DynamoRequest.WithTableReference
import d4s.models.table.TableReference
import software.amazon.awssdk.services.dynamodb.model._

import scala.jdk.CollectionConverters._

final case class UpdateTableTags(
  table: TableReference,
  dynamoResourceName: String,
  tagsToAdd: Map[String, String] = Map.empty,
) extends DynamoRequest
  with WithTableReference[UpdateTableTags] {

  override type Rq  = TagResourceRequest
  override type Rsp = TagResourceResponse

  override def withTableReference(t: TableReference => TableReference): UpdateTableTags = copy(table = t(table))

  def withTags(t: Map[String, String]): UpdateTableTags = copy(tagsToAdd = tagsToAdd ++ t)

  def toAmz: TagResourceRequest = {
    val tags = (table.tags ++ tagsToAdd).map {
      case (k, v) => Tag.builder().key(k).value(v).build()
    }.asJavaCollection

    TagResourceRequest
      .builder()
      .resourceArn(dynamoResourceName)
      .tags(tags)
      .build()
  }
}
