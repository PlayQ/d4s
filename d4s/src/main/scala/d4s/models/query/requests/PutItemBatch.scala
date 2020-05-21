package d4s.models.query.requests

import d4s.codecs.{D4SAttributeEncoder, D4SEncoder}
import d4s.models.query.DynamoRequest
import d4s.models.query.DynamoRequest.{BatchWriteEntity, DynamoWriteBatchRequest, WithBatch, WithTableReference}
import d4s.models.table.TableReference
import software.amazon.awssdk.services.dynamodb.model._

import scala.jdk.CollectionConverters._

final case class PutItemBatch(
  table: TableReference,
  batchItems: List[Map[String, AttributeValue]] = Nil,
) extends DynamoWriteBatchRequest
  with WithTableReference[PutItemBatch]
  with WithBatch[PutItemBatch, DynamoRequest.BatchWriteEntity] {

  override def withBatch[I: D4SEncoder](batchItems: List[DynamoRequest.BatchWriteEntity[I]]): PutItemBatch = {
    withBatch {
      table.ttlField match {
        case Some(ttlName) =>
          batchItems.map {
            case BatchWriteEntity(item, ttl) =>
              val mbTTL = ttl.map(D4SAttributeEncoder.encodeField(ttlName, _))
              D4SEncoder[I].encodeObject(item) ++ mbTTL.getOrElse(Map.empty)
          }

        case None =>
          batchItems.map(i => D4SEncoder[I].encodeObject(i.item))
      }
    }
  }

  override def withBatch(batchItems: List[Map[String, AttributeValue]]): PutItemBatch = copy(batchItems = batchItems)

  override def withTableReference(t: TableReference => TableReference): PutItemBatch = copy(table = t(table))

  override def toAmz: BatchWriteItemRequest = {
    val items = batchItems.map(ll => WriteRequest.builder().putRequest(PutRequest.builder().item(ll.asJava).build()).build())
    BatchWriteItemRequest
      .builder()
      .requestItems(Map(table.fullName -> items.asJava).asJava)
      .build()
  }
}
