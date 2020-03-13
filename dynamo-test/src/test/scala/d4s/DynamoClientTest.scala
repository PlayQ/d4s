package d4s

import d4s.DynamoClientTest.Ctx
import d4s.env.{DynamoRnd, DynamoTestBase}
import zio.IO

object DynamoClientTest {
  final case class Ctx(dynamo: DynamoClient[IO])
}

final class DynamoClientTest extends DynamoTestBase[Ctx] with DynamoRnd {

  "list tables" in scopeIO {
    ctx =>
      import ctx._

      for {
        response <- dynamo.raw(_.listTables())
        _        <- assertIO(response != null)
        _        <- assertIO(response.tableNames != null)
      } yield ()
  }
}
