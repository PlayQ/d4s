package d4s.models.query.requests

import java.util

import d4s.compat.chaining._
import d4s.models.query.DynamoRequest
import d4s.models.query.DynamoRequest.WithTableReference
import d4s.models.table.{TableDDL, TableReference}
import software.amazon.awssdk.services.dynamodb.model._

import scala.jdk.CollectionConverters._

final case class CreateTable(
  table: TableReference,
  ddl: TableDDL,
) extends DynamoRequest
  with WithTableReference[CreateTable] {

  override type Rq  = CreateTableRequest
  override type Rsp = CreateTableResponse

  override def withTableReference(t: TableReference => TableReference): CreateTable = copy(table = t(table))

  def toAmz: CreateTableRequest = {

    val allAttributes: Seq[AttributeDefinition] = {
      Seq(
        ddl.key.toAttributes,
        ddl.globalIndexes.flatMap(_.key.toAttributes),
        ddl.localIndexes.flatMap(_.key.toAttributes),
        ddl.additionalAttributes.map(_.toAttribute),
      ).flatten.distinct
    }

    val globalIndexesRaw: util.Collection[GlobalSecondaryIndex] = {
      ddl.globalIndexes.map(i => i.toAmz(ddl.provisioning.getIndexProvisioning(i.name))).asJavaCollection
    }

    val localIndexesRaw: util.Collection[LocalSecondaryIndex] = {
      ddl.localIndexes.map(_.toAmz).asJavaCollection
    }

    CreateTableRequest
      .builder()
      .tableName(table.fullName)
      .keySchema(ddl.key.toJava)
      .pipe(if (globalIndexesRaw.isEmpty) identity else _.globalSecondaryIndexes(globalIndexesRaw))
      .pipe(if (localIndexesRaw.isEmpty) identity else _.localSecondaryIndexes(localIndexesRaw))
      .attributeDefinitions(allAttributes.asJavaCollection)
      .pipe(ddl.provisioning.tableProvisioning.configure(_))
      .build()
  }
}
