package d4s

import d4s.env.Models._
import d4s.env.{DynamoRnd, DynamoTestBase}

@SuppressWarnings(Array("DoubleNegation"))
final class LiteralRequestsTest extends DynamoTestBase[0] with DynamoRnd {

  "literal tuple codecs" should {

    "perform put" in {
      (testTable4: TestTable4) =>
        for {
          key   <- randomIO[Int].map(i => s"key$i")
          _     <- d4z.runUnrecorded(testTable4.table.putItem.withItem(("key1", key)).withItem(("value1", "f3")))
          get    = testTable4.table.getItem(("key1", key)).decodeItem[("value1", String)]
          read1 <- d4z.runUnrecorded(get)
          _     <- assertIO(read1 contains ("value1" -> "f3"))
        } yield ()
    }

  }

}
