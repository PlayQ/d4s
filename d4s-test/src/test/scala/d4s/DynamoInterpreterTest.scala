package d4s

import java.util.UUID
import d4s.DynamoInterpreterTest.Ctx
import d4s.codecs.{AttributeNames, D4SAttributeEncoder, D4SCodec, WithD4S}
import d4s.env.Models._
import d4s.env.{DynamoRnd, DynamoTestBase}
import d4s.implicits._
import d4s.models.{DynamoException, OffsetLimit}
import d4s.models.query.DynamoRequest.BatchWriteEntity
import d4s.models.query.{DynamoQuery, DynamoRequest}
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import zio.interop.catz._
import zio.{IO, Ref, Task, ZIO}

object DynamoInterpreterTest {

  final case class Ctx(
    connector: DynamoConnector[IO],
    testTable: InterpreterTestTable,
    countersTable: TestTable1,
    currentTable: UpdatedTestTable,
    newTable1: UpdatedTestTable1,
    newTable2: UpdatedTestTable2,
  )

}

@SuppressWarnings(Array("DoubleNegation"))
final class DynamoInterpreterTest extends DynamoTestBase[Ctx] with DynamoRnd {

  "dynamo interpreter" should {

    "perform put" in scopeIO {
      ctx =>
        import ctx._
        val payload = InterpreterTestPayload("perform put", 12, "f3", RandomPayload("f2"))

        for {
          _     <- connector.runUnrecorded(testTable.table.putItem(payload))
          get    = testTable.table.getItem(payload.key).decodeItem[InterpreterTestPayload]
          read1 <- connector.runUnrecorded(get)
          _     <- assertIO(read1.contains(payload))
        } yield ()
    }

    "perform delete" in scopeIO {
      ctx =>
        import ctx._
        val payload = InterpreterTestPayload("perform delete", 22, "f3", RandomPayload("f2"))
        for {
          _     <- connector.runUnrecorded(testTable.table.putItem(payload))
          get    = testTable.table.getItem(payload.key).decodeItem[InterpreterTestPayload]
          read1 <- connector.runUnrecorded(get)
          _     <- assertIO(read1.contains(payload))

          _     <- connector.runUnrecorded(testTable.table.deleteItem(payload.key))
          read2 <- connector.runUnrecorded(get)
          _     <- assertIO(read2.isEmpty)
        } yield ()
    }

    "perform update" in scopeIO {
      ctx =>
        import ctx._
        val payload1 = InterpreterTestPayload("perform update", 3, "f3", RandomPayload("f2"))
        val payload2 = InterpreterTestPayload("perform update", 3, "f33", RandomPayload("f5"))
        for {
          _     <- connector.runUnrecorded(testTable.table.putItem(payload1))
          get    = testTable.table.getItem(payload1.key).decodeItem[InterpreterTestPayload]
          read1 <- connector.runUnrecorded(get)
          _     <- assertIO(read1.contains(payload1))

          _     <- connector.runUnrecorded(testTable.table.updateItem(payload2))
          read2 <- connector.runUnrecorded(get)
          _     <- assertIO(read2.contains(payload2))

        } yield ()
    }

    "perform insert if not exists" in scopeIO {
      ctx =>
        import ctx._
        val payload1 = InterpreterTestPayload("perform insert if not exists", 4, "f3", RandomPayload("f2"))
        val payload2 = InterpreterTestPayload("perform insert if not exists", 4, "f33", RandomPayload("f5"))
        for {
          _   <- connector.runUnrecorded(testTable.table.putItem(payload1))
          res <- connector.runUnrecorded(testTable.table.putItem.withItem(payload2).ifNotExists().optConditionFailure)
          _   <- assertIO(res.isDefined)

          get    = testTable.table.getItem(payload1.key).decodeItem[InterpreterTestPayload]
          read1 <- connector.runUnrecorded(get)
          _     <- assertIO(read1.contains(payload1))

          _     <- connector.runUnrecorded(testTable.table.putItem(payload2))
          read2 <- connector.runUnrecorded(get)
          _     <- assertIO(read2.contains(payload2))
        } yield ()
    }

    "perform delete if exists" in scopeIO {
      ctx =>
        import ctx._
        val payload = InterpreterTestPayload("perform delete if exists", 5, "f3", RandomPayload("f2"))
        for {
          _     <- connector.runUnrecorded(testTable.table.putItem(payload))
          get    = testTable.table.getItem(payload.key).decodeItem[InterpreterTestPayload]
          read1 <- connector.runUnrecorded(get)
          _     <- assertIO(read1.contains(payload))

          delete = testTable.table.deleteItem(payload.key)
          _     <- connector.runUnrecorded(delete.ifExists())
          res   <- connector.runUnrecorded(delete.ifExists().optConditionFailure)
          _     <- assertIO(res.isDefined)

          read2 <- connector.runUnrecorded(get)
          _     <- assertIO(read2.isEmpty)
        } yield ()
    }

    "perform delete if attribute begins with some string" in scopeIO {
      ctx =>
        import ctx._

        val payload = InterpreterTestPayload("perform delete if attribute begins with some string", 5, "prefix-f3", RandomPayload("f2"))

        for {
          _        <- connector.runUnrecorded(testTable.table.putItem(payload))
          get       = testTable.table.getItem(payload.key).decodeItem[InterpreterTestPayload]
          testRead <- connector.runUnrecorded(get)
          _        <- assertIO(testRead.get == payload)

          delete = testTable.table.deleteItem(payload.key)
          _     <- connector.runUnrecorded(delete.withCondition("field1".of[String] beginsWith "p"))

          testRead2 <- connector.runUnrecorded(get)
          _         <- assertIO(testRead2.isEmpty)
        } yield ()
    }

    "perform put/get/delete batch " in scopeIO {
      ctx =>
        import ctx._
        val randomPayload = RandomPayload("f2")
        val stressed = IO.foreach((1 to 200).toList) {
          indx =>
            IO(InterpreterTestPayload("perform put/get/delete batch", indx, "xxx", randomPayload))
        }

        val putBatch         = stressed.map(_.map(BatchWriteEntity(_)))
        val getOrDeleteBatch = stressed.map(_.map(_.key))
        for {
          newAll         <- stressed
          putStressBatch <- putBatch
          getStressBatch <- getOrDeleteBatch
          _              <- connector.runUnrecorded(testTable.table.putItemBatch(putStressBatch))
          scan            = testTable.table.scan.decodeItems[InterpreterTestPayload]
          read1          <- connector.runUnrecorded(scan)
          _              <- assertIO(newAll.forall(el => read1.contains(el)))

          read2 <- connector.runUnrecorded(testTable.table.getItemBatch(getStressBatch).decodeItems[InterpreterTestPayload])
          _     <- assertIO(newAll.forall(read2.contains))

          _     <- connector.runUnrecorded(testTable.table.deleteItemBatch(getStressBatch))
          read3 <- connector.runUnrecorded(scan)
          _     <- assertIO(newAll.forall(!read3.contains(_)))
        } yield ()
    }

    "perform update with condition" in scopeIO {
      ctx =>
        import ctx._
        val payload1         = InterpreterTestPayload("perform update with condition", 9, "f3", RandomPayload("f22"))
        val modifiedPayload1 = InterpreterTestPayload("perform update with condition", 9, "f33", RandomPayload("3123"))
        for {
          _ <- connector.runUnrecorded(testTable.table.putItem(payload1))

          res <- connector.runUnrecorded(testTable.table.updateItem(modifiedPayload1).withCondition("field3".of[String] === "f7").optConditionFailure)
          _   <- assertIO(res.isDefined)

          _    <- connector.runUnrecorded(testTable.table.updateItem(modifiedPayload1).withCondition("field3".of[String] === "f3"))
          scan  = testTable.table.scan.decodeItems[InterpreterTestPayload]
          read <- connector.runUnrecorded(scan.execPagedFlatten())
          _    <- assertIO(read.contains(modifiedPayload1))
        } yield ()
    }

    "perform update with null and not null condition" in scopeIO {
      ctx =>
        import ctx._

        final case class NullableField(field4: Option[String])
        object NullableField extends WithD4S[NullableField]
        final case class NullTestPayload(payload: InterpreterTestPayload, nullableField: NullableField)
        object NullTestPayload {
          implicit val codec: D4SCodec[NullTestPayload] =
            InterpreterTestPayload.codec.imap2(NullableField.codec)(NullTestPayload(_, _))(ext => ext.payload -> ext.nullableField)
          implicit val attrNames: AttributeNames[NullTestPayload] = AttributeNames[InterpreterTestPayload] ++ AttributeNames[NullableField]
        }

        val payload1 = InterpreterTestPayload("perform update with null condition", 9, "f3", RandomPayload("f22"))
        for {
          _ <- connector.runUnrecorded(testTable.table.putItem(payload1))

          _    <- connector.runUnrecorded(
            testTable.table
              .updateItem(payload1.key)
              .withCondition("p".notNull)
              .withUpdateExpression("SET field4 = :nullField")
              .withAttributeValues(":nullField" -> AttributeValue.builder().nul(true).build())
          )

          _    <- connector.runUnrecorded(
            testTable.table
              .updateItem(payload1.key)
              .withCondition("field4".isNull)
              .withUpdateExpression("SET field4 = :notnullField")
              .withAttributeValues(":notnullField" -> D4SAttributeEncoder.encode(Some("not null"): Option[String]))
          )
          scan  = testTable.table.scan.decodeItems[NullTestPayload]
          read <- connector.runUnrecorded(scan.execPagedFlatten())
          _ <- assertIO(read.exists(_.nullableField.field4 == Some("not null")))
        } yield ()
    }

    "perform update with complex condition" in scopeIO {
      ctx =>
        import ctx._

        val payload1         = InterpreterTestPayload("perform update with complex condition", 3, "f3", RandomPayload("f22"))
        val modifiedPayload1 = InterpreterTestPayload("perform update with complex condition", 3, "f33", RandomPayload("3123"))

        val complexCondition1 = "field2".of[Int].between(2, 4) && ("field3".of[String] === "f4" || "field3".of[String] === "f12")
        val complexCondition2 = "field2".of[Int].between(2, 4) && ("field3".of[String] === "f3" || "field3".of[String] === "f12")

        for {
          _ <- connector.runUnrecorded(testTable.table.putItem(payload1))

          res <- connector.runUnrecorded(testTable.table.updateItem(modifiedPayload1).withCondition(complexCondition1).optConditionFailure)
          _   <- assertIO(res.isDefined)

          _    <- connector.runUnrecorded(testTable.table.updateItem(modifiedPayload1).withCondition(complexCondition2))
          scan  = testTable.table.scan.decodeItems[InterpreterTestPayload]
          read <- connector.runUnrecorded(scan.execPagedFlatten())
          _    <- assertIO(read.contains(modifiedPayload1))
        } yield ()
    }

    "perform update with updateExpression" in scopeIO {
      ctx =>
        import ctx._
        import ctx.testTable.table

        final case class AdditionalFields(field4: String, field5: Int)
        object AdditionalFields extends WithD4S[AdditionalFields]
        final case class ExtendedPayload(payload: InterpreterTestPayload, additionalFields: AdditionalFields)
        object ExtendedPayload {
          implicit val codec: D4SCodec[ExtendedPayload] =
            InterpreterTestPayload.codec.imap2(AdditionalFields.codec)(ExtendedPayload(_, _))(ext => ext.payload -> ext.additionalFields)
          implicit val attrNames: AttributeNames[ExtendedPayload] = AttributeNames[InterpreterTestPayload] ++ AttributeNames[AdditionalFields]
        }

        val key              = InterpreterTestKey("perform update with updateExpression", 3)
        val payload          = InterpreterTestPayload(key)("f3", RandomPayload("f22"))
        val modifiedPayload1 = InterpreterTestPayload(key)("f3", RandomPayload("asdf456456"))

        val complexCondition = "field2".of[Int] < 5 && (!(!"field3".existsField) && "field4".notExists) && !("field3".of[String] < "f12")

        println(complexCondition.eval.conditionExpression)

        for {
          _ <- connector.runUnrecorded(table.putItem(payload))

          _ <- connector.runUnrecorded {
            table
              .updateItem(modifiedPayload1)
              .withCondition(complexCondition)
              .withUpdateExpression("SET field4 = :field4, field5 = :field5 REMOVE field9")
              .withAttributeValues(":field4" -> D4SAttributeEncoder.encode("FIELD4"))
              .withAttributeValues(":field5" -> D4SAttributeEncoder.encode(8))
              .optConditionFailure
          }.flatMap(failure => IO.effectTotal(assert(failure.isEmpty)))

          _ <- connector.runUnrecorded {
            table.updateItem(modifiedPayload1).withCondition(complexCondition).optConditionFailure
          }.flatMap(failure => IO.effectTotal(failure.isDefined))

          item1 <- connector.runUnrecorded(table.getItem(key).decodeItem[ExtendedPayload])
          _     <- assertIO(item1.contains(ExtendedPayload(modifiedPayload1, AdditionalFields("FIELD4", 8))))

          _ <- connector.runUnrecorded {
            table
              .updateItem(key)
              .withUpdateExpression("SET field4 = :field4")
              .withAttributeValues(":field4" -> D4SAttributeEncoder.encode("X"))
              .optConditionFailure
          }.flatMap(failure => IO.effectTotal(failure.isEmpty))

          item2 <- connector.runUnrecorded(table.getItem(key).decodeItem[ExtendedPayload])
          _     <- assertIO(item2.contains(ExtendedPayload(modifiedPayload1, AdditionalFields("X", 8))))
        } yield ()
    }

    "perform ranges read" in scopeIO {
      ctx =>
        import ctx._
        val hash     = "ranges read-delete"
        val range1   = 111
        val range2   = 122
        val payload1 = InterpreterTestPayload(hash, range1, "f3", RandomPayload("f2"))
        val payload2 = InterpreterTestPayload(hash, range2, "f3", RandomPayload("f2"))

        for {
          _ <- connector.runUnrecorded(testTable.table.putItem(payload1))

          query = testTable.table.query.withKey(Map("field1" -> D4SAttributeEncoder.encode(hash))).decodeItems[InterpreterTestPayload]
          res1 <- connector.runUnrecorded(query)
          _    <- assertIO(res1.size == 1)
          _    <- connector.runUnrecorded(testTable.table.putItem(payload2))

          res2 <- connector.runUnrecorded(query)
          _    <- assertIO(res2.size == 2)

          _    <- connector.runUnrecorded(testTable.table.deleteItem(payload1.key))
          res3 <- connector.runUnrecorded(query)
          _    <- assertIO(res3.size == 1)

          _    <- connector.runUnrecorded(testTable.table.deleteItem(payload2.key))
          res4 <- connector.runUnrecorded(query)
          _    <- assertIO(res4.isEmpty)
        } yield ()
    }

    "perform queries on index" in scopeIO {
      ctx =>
        import ctx._
        val payload = InterpreterTestPayload("index_test", 333, "f3", RandomPayload("f2"))
        for {
          _ <- connector.runUnrecorded(testTable.table.putItem(payload))
          get =
            testTable.table.query
              .withIndex(testTable.globalIndex)
              .withKey(testTable.globalIndex.key.bind("index_test", "f3"))
              .decodeItems[InterpreterTestPayload]
          read1 <- connector.runUnrecorded(get)
          _     <- assertIO(read1.size == 1)
          _     <- assertIO(read1.head == payload)
        } yield ()
    }

    "perform queries with conditions" in scopeIO {
      ctx =>
        import ctx._
        val payload = InterpreterTestPayload("query_conditions_test", 333, "f3", RandomPayload("f2"))
        for {
          _ <- connector.runUnrecorded(testTable.table.putItem(payload))
          get1 =
            testTable.table.query
              .withKey(testTable.mainKey.bind("query_conditions_test"))
              .withCondition("field2".of[Int] > 332)
              .decodeItems[InterpreterTestPayload]
          read1 <- connector.runUnrecorded(get1)
          _     <- assertIO(read1.size == 1)
          _     <- assertIO(read1.head == payload)

          get2 =
            testTable.table.query
              .withKey(testTable.mainKey.bind("query_conditions_test"))
              .withCondition("field2".of[Int] < 332)
              .decodeItems[InterpreterTestPayload]
          read2 <- connector.runUnrecorded(get2)
          _     <- assertIO(read2.isEmpty)
        } yield ()
    }

    "perform paged execution" in scopeIO {
      ctx =>
        import ctx._
        val payload      = InterpreterTestPayload("paging_test", 333, "f3", RandomPayload("f2"))
        val prefix       = UUID.randomUUID()
        val put          = testTable.table.putItem.withPrefix(prefix)
        val expectedSize = 100
        for {
          _ <- IO.foreachParN(3)((1 to expectedSize).toList)(i => connector.runUnrecorded(put.withItem(payload.copy(field2 = i)).retryWithPrefix(testTable.ddl)))
          get =
            testTable.table.query
              .withKey(testTable.mainKey.bind("paging_test"))
              .withLimit(10)
              .withPrefix(prefix)
              .decodeItems[InterpreterTestPayload]
              .execPagedFlatten()
              .retryWithPrefix(testTable.ddl)

          read1 <- connector.runUnrecorded(get)
          _     <- assertIO(read1.size == expectedSize)
        } yield ()
    }

    "perform streamed execution" in scopeIO {
      ctx =>
        import ctx._
        val payload      = InterpreterTestPayload("streaming_test", 333, "f3", RandomPayload("f2"))
        val prefix       = UUID.randomUUID()
        val put          = testTable.table.putItem.withPrefix(prefix)
        val expectedSize = 100
        for {
          _   <- IO.foreachParN(3)((1 to expectedSize).toList)(i => connector.runUnrecorded(put.withItem(payload.copy(field2 = i)).retryWithPrefix(testTable.ddl)))
          ref <- Ref.make(Set.empty[InterpreterTestPayload])
          get =
            testTable.table.query
              .withKey(testTable.mainKey.bind("streaming_test"))
              .withLimit(10)
              .withPrefix(prefix)
              .decodeItems[InterpreterTestPayload]
              .execStreamed
              .retryWithPrefix(testTable.ddl)
          _ <-
            connector
              .runUnrecorded(get)
              .evalMap[IO[Throwable, _], Unit](res => ref.update(_ ++ res)).compile.drain
          all <- ref.get
          _   <- assertIO(all.size == expectedSize)

          get2OnUncreatedTable =
            testTable.table.query
              .withKey(testTable.mainKey.bind("streaming_test"))
              .withPrefix(UUID.randomUUID())
              .decodeItems[InterpreterTestPayload]
              .execStreamed
              .retryWithPrefix(testTable.ddl)
          _ <- connector.runUnrecorded(get2OnUncreatedTable).covary[Task].compile.drain
        } yield ()
    }

    "perform offset operations" in scopeIO {
      ctx =>
        import ctx._
        val payload      = InterpreterTestPayload("offset_test", 333, "f3", RandomPayload("f2"))
        val prefix       = UUID.randomUUID()
        val put          = testTable.table.putItem.withPrefix(prefix)
        val expectedSize = 20
        for {
          _ <- IO.foreachParN(3)((1 to expectedSize).toList) {
            i =>
              connector.runUnrecorded(put.withItem(payload.copy(field2 = i)).retryWithPrefix(testTable.ddl))
          }
          get =
            testTable.table.query
              .withKey(testTable.mainKey.bind("offset_test"))
              .withPrefix(prefix)
              .decodeItems[InterpreterTestPayload]
              .execOffset(OffsetLimit(13, 5))
              .retryWithPrefix(testTable.ddl)
          all <- connector.runUnrecorded(get)
          _    = all.foreach(println)
          _   <- assertIO(all.size == 5)
        } yield ()
    }

    "perform batched operations execution" in scopeIO {
      ctx =>
        import ctx._
        val payload      = InterpreterTestPayload("batch_test", 333, "f3", RandomPayload("f2"))
        val prefix       = UUID.randomUUID()
        val expectedSize = 100
        for {
          items <- IO.foreachParN(3)((1 to expectedSize).toList)(i => IO(BatchWriteEntity(payload.copy(field2 = i))))
          put =
            testTable.table
              .putItemBatch(items)
              .withPrefix(prefix)
              .retryWithPrefix(testTable.ddl)
          _ <- connector.runUnrecorded(put)

          get =
            testTable.table.query
              .withPrefix(prefix)
              .withKey(testTable.mainKey.bind("batch_test"))
              .decodeItems[InterpreterTestPayload]
              .execPagedFlatten()
              .retryWithPrefix(testTable.ddl)

          read1 <- connector.runUnrecorded(get)
          _     <- assertIO(read1.size == expectedSize)
          _     <- assertIO(read1.toSet == items.map(_.item).toSet)

          delete =
            testTable.table
              .deleteItemBatch(items.map(_.item.key))
              .withPrefix(prefix)
              .retryWithPrefix(testTable.ddl)
          _ <- connector.runUnrecorded(delete)

          read2 <- connector.runUnrecorded(get)
          _     <- assertIO(read2.isEmpty)
        } yield ()
    }

    "describe/update tags" in scopeIO {
      ctx =>
        import ctx._
        for {
          arn <- connector.runUnrecorded(testTable.table.describe).map(_.table().tableArn())
          _   <- connector.runUnrecorded(testTable.table.updateTags(arn, Map("someTags" -> "test")))
          _   <- connector.runUnrecorded(testTable.table.markForDeletion(arn))
        } yield ()
    }

    "add, update counter field" in scopeIO {
      ctx =>
        import ctx._
        val v1     = UUID.randomUUID().toString
        val v2     = UUID.randomUUID().toString
        val prefix = UUID.randomUUID()

        for {
          _ <- connector.run("add value") {
            countersTable.table
              .updateItem(Table1Key(v1, v2))
              .withUpdateExpression("ADD counterField :value")
              .withAttributeValues(":value" -> D4SAttributeEncoder.encode(0L))
              .withPrefix(prefix)
              .retryWithPrefix(countersTable.ddl)
          }

          _ <- connector.run("add value") {
            countersTable.table
              .updateItem(Table1Key(v1, v2))
              .withUpdateExpression("ADD counterField :value")
              .withAttributeValues(":value" -> D4SAttributeEncoder.encode(1L))
              .withPrefix(prefix)
              .retryWithPrefix(countersTable.ddl)
          }

          res <-
            connector
              .run("get value") {
                countersTable.table
                  .getItem(Table1Key(v1, v2))
                  .decodeItem[Table1ItemWithCounter]
                  .withPrefix(prefix)
                  .retryWithPrefix(countersTable.ddl)
              }.map(_.map(_.counterField).getOrElse(-1L))

          _ <- assertIO(res == 1L)
        } yield ()
    }

    "perform table update" in scopeIO {
      ctx =>
        import ctx._
        import currentTable._
        import d4s.models.table.index.ProvisionedGlobalIndex._
        import zio.duration.Duration.fromScala

        import scala.concurrent.duration._
        import scala.jdk.CollectionConverters._

        def repeatableUpdateQuery[DR <: DynamoRequest, Dec](query: DynamoQuery[DR, Dec]) = {
          def retryPolicy(attempts: Int): ZIO[Any, DynamoException, Unit] = {
            if (attempts == 0)
              ZIO.unit
            else {
              connector
                .run("repeatable-query-run")(query).foldM(
                  _ =>
                    ZIO.sleep(fromScala(3.second)).provideLayer(zio.clock.Clock.live) *>
                    retryPolicy(attempts - 1),
                  _ => ZIO.unit,
                )
            }
          }
          retryPolicy _
        }

        for {
          _ <- connector.run("update-index") {
            table.update.withIndexToCreate(newTable1.globalIndex.toProvisionedIndex(newTable1.ddl.provisioning))
          }
          res <- connector.run("describe")(table.describe)
          set  = Set("key1", "key2", "key3")
          _   <- assertIO(res.table().globalSecondaryIndexes().asScala.nonEmpty)
          _   <- assertIO(res.table().globalSecondaryIndexes().asScala.size == 1)
          _   <- assertIO(res.table().attributeDefinitions().asScala.map(_.attributeName()).forall(set.contains))

          queryToRepeat = table.update.withIndexToCreate(newTable2.globalIndex2.toProvisionedIndex(newTable2.ddl.provisioning))
          policy        = repeatableUpdateQuery(queryToRepeat)
          _            <- policy(10)

          res2 <- connector.run("describe-2")(table.describe)
          set2  = Set("key1", "key2", "key3", "key4")
          _    <- assertIO(res2.table().globalSecondaryIndexes().asScala.nonEmpty)
          _    <- assertIO(res2.table().globalSecondaryIndexes().asScala.size == 2)
          _    <- assertIO(res2.table().attributeDefinitions().asScala.map(_.attributeName()).forall(set2.contains))
        } yield ()
    }

    "perform streamed DeleteBatch request" in scopeIO {
      ctx =>
        import ctx._
        val payload      = InterpreterTestPayload("batch_test", 333, "f3", RandomPayload("f2"))
        val prefix       = UUID.randomUUID()
        val expectedSize = 100
        for {
          items <- IO.foreachParN(3)((1 to expectedSize).toList)(i => IO(BatchWriteEntity(payload.copy(field2 = i))))
          put =
            testTable.table
              .putItemBatch(items)
              .withPrefix(prefix)
              .retryWithPrefix(testTable.ddl)
          _ <- connector.runUnrecorded(put)

          get =
            testTable.table.query
              .withPrefix(prefix)
              .withKey(testTable.mainKey.bind("batch_test"))
              .decodeItems[InterpreterTestPayload]
              .execPagedFlatten()
              .retryWithPrefix(testTable.ddl)

          read1 <- connector.runUnrecorded(get)
          _     <- assertIO(read1.size == expectedSize)
          _     <- assertIO(read1.toSet == items.map(_.item).toSet)

          deleteQuery =
            testTable.table
              .queryDeleteBatch(testTable.mainKey.bind("batch_test"))
              .withPrefix(prefix)
              .retryWithPrefix(testTable.ddl)
          _ <- connector.runUnrecorded(deleteQuery)

          read2 <- connector.runUnrecorded(get)
          _     <- assertIO(read2.isEmpty)

          _ <- connector.runUnrecorded(put)

          read3 <- connector.runUnrecorded(get)
          _     <- assertIO(read3.size == expectedSize)
          _     <- assertIO(read3.toSet == items.map(_.item).toSet)

          deleteScan =
            testTable.table.scanDeleteBatch
              .withPrefix(prefix)
              .retryWithPrefix(testTable.ddl)
          _ <- connector.runUnrecorded(deleteScan)

          read4 <- connector.runUnrecorded(get)
          _     <- assertIO(read4.isEmpty)
        } yield ()
    }

    "perform streamed DeleteBatch request on non-existent table with retryWithPrefix" in scopeIO {
      ctx =>
        import ctx._
        val prefix1 = UUID.randomUUID()
        val prefix2 = UUID.randomUUID()

        for {
          _ <- connector.runUnrecorded(
            testTable.table
              .queryDeleteBatch(testTable.mainKey.bind("batch_test"))
              .withPrefix(prefix1)
              .retryWithPrefix(testTable.ddl)
          )
          _ <- connector.runUnrecorded(
            testTable.table.scanDeleteBatch
              .withPrefix(prefix2)
              .retryWithPrefix(testTable.ddl)
          )
        } yield ()
    }

    "filter by number set" in scopeIO {
      ctx =>
        import ctx._
        val payload = InterpreterTestPayload("number_test", 333, "f4", RandomPayload("f2"))
        for {
          _ <- connector.runUnrecorded(testTable.table.putItem(payload))
          read1 <- connector.runUnrecorded {
            testTable.table
              .query(testTable.globalIndex, "number_test", "f4")
              .withFilterExpression(List("p", "randomArray").of[List[Int]] contains payload.p.randomArray.head)
              .withFilterExpression(List("p", "field1").of[List[String]] contains "f2")
              .decodeItems[InterpreterTestPayload]
              .execPagedFlatten()
          }
          _ <- assertIO(read1.size == 1)
          _ <- assertIO(read1.head == payload)
          read2 <- connector.runUnrecorded {
            testTable.table
              .query(testTable.globalIndex, "number_test", "f4")
              .withFilterExpression(!(List("p", "randomArray").of[List[Int]] contains payload.p.randomArray.head))
              .decodeItems[InterpreterTestPayload]
              .execPagedFlatten()
          }
          _ <- assertIO(read2.isEmpty)
        } yield ()
    }

    "DynamoConnectorLocal (d4z) should perform actions" in scopeZIO {
      ctx =>
        import ctx._
        val payload = InterpreterTestPayload("perform put2", 321, "f3", RandomPayload("f2"))
        for {
          _     <- d4z.runUnrecorded(testTable.table.putItem(payload))
          get    = testTable.table.getItem(payload.key).decodeItem[InterpreterTestPayload]
          read1 <- d4z.runUnrecorded(get)
          _     <- assertIO(read1.contains(payload))
        } yield ()
    }

  }

}
