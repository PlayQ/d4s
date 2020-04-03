package d4s

import java.util.UUID

import d4s.DDLServiceTest.Ctx
import d4s.config.DynamoMeta
import d4s.env.Models._
import d4s.env.{DynamoRnd, DynamoTestBase}
import d4s.models.DynamoExecution
import d4s.models.table.TableDef
import zio.IO

object DDLServiceTest {

  final case class Ctx(
    meta: DynamoMeta,
    ddl: DynamoDDLService[IO],
    connector: DynamoConnector[IO],
    dynamoClient: DynamoClient[IO],
    instances: Set[TableDef],
    testTable: TestTable1
  )

}

final class DDLServiceTest extends DynamoTestBase[Ctx] with DynamoRnd {
  "ddl service " must {
    "perform ddl up" in scopeIO {
      ctx =>
        import ctx._
        for {
          prefix     <- randomIO[UUID]
          _          <- assertIO(instances.nonEmpty)
          randomKey  = Table1Key(UUID.randomUUID().toString, UUID.randomUUID().toString)
          randomItem = Table1Item(UUID.randomUUID().toString, UUID.randomUUID().toString)

          put = testTable.table.putItem.withItems(randomKey, randomItem).withPrefix(prefix)
          _   <- connector.runUnrecorded(put.retryWithPrefix(testTable.ddl))
          get = testTable.table.getItem(randomKey).withPrefix(prefix)

          item <- connector.runUnrecorded(get.decodeItem[Table1Item].retryWithPrefix(testTable.ddl))
          _    <- assertIO(item.get == randomItem)

          createdTables = instances.map(_.table.fullName)

          tablesFromAws <- connector.runUnrecorded(DynamoExecution.listTables.map(_.toSet))

          _ <- assertIO(tablesFromAws.intersect(createdTables + put.table.fullName).nonEmpty)
        } yield {}
    }
  }
}
