package d4s.env

import java.util.UUID

import d4s.codecs.{D4SCodec, WithD4S}
import d4s.config.DynamoMeta
import d4s.models.table.index.GlobalIndex
import d4s.models.table.{DynamoKey, TableDDL, TableDef, TableReference}
import izumi.fundamentals.platform.language.Quirks
import software.amazon.awssdk.services.dynamodb.model.{Projection, ProjectionType}

import scala.util.Random

object Models {
  private def tableName() = s"t1${UUID.randomUUID()}"

  final case class Table1Key(key1: String, key2: String)
  object Table1Key extends WithD4S[Table1Key]

  final case class Table1Item(v1: String, v2: String)
  object Table1Item extends WithD4S[Table1Item]

  final case class Table1ItemWithCounter(key1: String, key2: String, counterField: Long)
  object Table1ItemWithCounter extends WithD4S[Table1ItemWithCounter]

  class TestTable1(implicit dynamoMeta: DynamoMeta) extends TableDef {
    override val table: TableReference = TableReference[String, String]("table1")("key1", "key2")
    override val ddl: TableDDL         = TableDDL(table)
  }

  class TestTable2(implicit dynamoMeta: DynamoMeta) extends TableDef {
    override val table: TableReference = TableReference[String, String]("table2")("key1", "key2")
    override val ddl: TableDDL         = TableDDL(table)
  }

  class TestTable3(implicit dynamoMeta: DynamoMeta) extends TableDef {
    override val table: TableReference = TableReference[String, String]("table3")("key1", "key2")
    override val ddl: TableDDL         = TableDDL(table)
  }

  class TestTable4(implicit dynamoMeta: DynamoMeta) extends TableDef {
    override val table: TableReference = TableReference[String]("table4")("key1")
    override val ddl: TableDDL         = TableDDL(table)
  }

  class UpdatedTestTable(implicit dynamoMeta: DynamoMeta) extends TableDef {
    override val table: TableReference = TableReference[String, String]("table_update")("key1", "key2")
    override val ddl: TableDDL         = TableDDL(table)
  }

  class UpdatedTestTable1(implicit dynamoMeta: DynamoMeta) extends TableDef {
    override val table: TableReference = TableReference[String, String]("table_update")("key1", "key2")

    val globalIndex: GlobalIndex[String, String] = GlobalIndex(
      "key3-to-key1",
      DynamoKey[String, String]("key1", "key3"),
      Projection
        .builder()
        .projectionType(ProjectionType.ALL)
        .build(),
    )

    override val ddl: TableDDL = TableDDL(table).withGlobalIndexes(globalIndex)
  }

  class UpdatedTestTable2(implicit dynamoMeta: DynamoMeta) extends TableDef {
    override val table: TableReference = TableReference[String, String]("table_update")("key1", "key2")

    val globalIndex: GlobalIndex[String, String] = GlobalIndex(
      "key3-to-key1",
      DynamoKey[String, String]("key1", "key3"),
      Projection
        .builder()
        .projectionType(ProjectionType.ALL)
        .build(),
    )

    val globalIndex2: GlobalIndex[String, String] = GlobalIndex(
      "key4-to-key2",
      DynamoKey[String, String]("key2", "key4"),
      Projection
        .builder()
        .projectionType(ProjectionType.ALL)
        .build(),
    )

    override val ddl: TableDDL = TableDDL(table).withGlobalIndexes(globalIndex, globalIndex2)
  }

  class InterpreterTestTable(implicit dynamoMeta: DynamoMeta) extends TableDef {
    val mainKey: DynamoKey[String, Int] = DynamoKey[String, Int]("field1", "field2")
    val globalIndex: GlobalIndex[String, String] = GlobalIndex(
      "field1-to-field3",
      DynamoKey[String, String]("field1", "field3"),
      Projection
        .builder()
        .projectionType(ProjectionType.ALL)
        .build(),
    )
    override val table: TableReference = TableReference(tableName(), mainKey)
    override val ddl: TableDDL = TableDDL(table)
      .withGlobalIndexes(globalIndex)
  }

  final case class InterpreterTestKey(field1: String, field2: Int)
  object InterpreterTestKey extends WithD4S[InterpreterTestKey]

  final case class InterpreterTestPayload(field1: String, field2: Int, field3: String, p: RandomPayload) {
    def key: InterpreterTestKey = InterpreterTestKey(field1, field2)
  }
  object InterpreterTestPayload extends WithD4S[InterpreterTestPayload] {
    def apply(field1: String, field2: Int)(field3: String, p: RandomPayload)(implicit dummy: DummyImplicit): InterpreterTestPayload = {
      Quirks.discard(dummy)
      new InterpreterTestPayload(field1, field2, field3, p)
    }
    def apply(key: InterpreterTestKey)(field3: String, p: RandomPayload): InterpreterTestPayload = {
      new InterpreterTestPayload(key.field1, key.field2, field3, p)
    }
  }

  final case class RandomPayload(field1: String, randomArray: Set[Int])
  object RandomPayload {
    def apply(field1: String): RandomPayload    = RandomPayload(field1, List.fill(10)(Random.nextInt(10)).toSet)
    implicit val codec: D4SCodec[RandomPayload] = D4SCodec.derived[RandomPayload]
  }
}
