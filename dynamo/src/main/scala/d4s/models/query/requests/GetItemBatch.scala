package d4s.models.query.requests

import d4s.codecs.circe.DynamoEncoder
import izumi.fundamentals.platform.functional.Identity
import d4s.models.query.DynamoRequest
import d4s.models.query.DynamoRequest.{WithBatch, WithTableReference}
import d4s.models.table.TableReference
import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, BatchGetItemRequest, BatchGetItemResponse, KeysAndAttributes}

import scala.jdk.CollectionConverters._

final case class GetItemBatch(
  table: TableReference,
  batchItems: List[Map[String, AttributeValue]] = Nil
) extends DynamoRequest
  with WithTableReference[GetItemBatch]
  with WithBatch[GetItemBatch, Identity] {

  override type Rq  = BatchGetItemRequest
  override type Rsp = List[BatchGetItemResponse]

  override def withBatch[I: DynamoEncoder](batchItems: List[I]): GetItemBatch = {
    withBatch(batchItems.map(DynamoEncoder[I].encode))
  }

  override def withBatch(batchItems: List[Map[String, AttributeValue]]): GetItemBatch = copy(batchItems = batchItems)

  override def withTableReference(t: TableReference => TableReference): GetItemBatch = copy(table = t(table))

  override def toAmz: BatchGetItemRequest = {
    val items = batchItems.map(_.asJava).asJava
    BatchGetItemRequest
      .builder()
      .requestItems(Map(table.fullName -> KeysAndAttributes.builder().keys(items).build()).asJava)
      .build()
  }

}
