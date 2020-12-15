package d4s

import d4s.models.DynamoExecution
import d4s.models.ExecutionStrategy.StrategyInput
import d4s.models.query.requests.{CreateBackup, DeleteTable}
import d4s.models.table.{TableDef, TablePrefix, TableReference}
import izumi.functional.bio.{Applicative2, Async2, F, Temporal2}
import logstage.LogIO2

import scala.collection.mutable
import scala.util.matching.Regex

trait DynamoTablesService[F[_, _]] {
  def create(tables: Set[TableDef]): F[Throwable, Unit]
  def createPrefixed[P: TablePrefix](prefix: P)(tables: Set[TableDef]): F[Throwable, Unit]

  def markForDeletion(tables: Set[TableDef]): F[Throwable, Unit]
  def markForDeletionPrefixed[P: TablePrefix](prefix: P)(tables: Set[TableDef]): F[Throwable, Unit]

  def delete(tables: Set[TableDef]): F[Throwable, Unit]
  def deletePrefixed[P: TablePrefix](prefix: P)(tables: Set[TableDef]): F[Throwable, Unit]

  def backup(tables: Set[TableDef]): F[Throwable, Unit]
  def backupPrefixed[P: TablePrefix](prefix: P)(tables: Set[TableDef]): F[Throwable, Unit]

  def listTables: F[Throwable, List[String]]
  def listTablesByRegex(regex: Regex): F[Throwable, List[String]]
}

object DynamoTablesService {

  final class Empty[F[+_, +_]: Applicative2] extends DynamoTablesService[F] {
    override def create(tables: Set[TableDef]): F[Throwable, Unit]                                             = F.unit
    override def createPrefixed[P: TablePrefix](prefix: P)(tables: Set[TableDef]): F[Throwable, Unit]          = F.unit
    override def markForDeletion(tables: Set[TableDef]): F[Throwable, Unit]                                    = F.unit
    override def markForDeletionPrefixed[P: TablePrefix](prefix: P)(tables: Set[TableDef]): F[Throwable, Unit] = F.unit
    override def listTables: F[Throwable, List[String]]                                                        = F.pure(List.empty)
    override def listTablesByRegex(regex: Regex): F[Throwable, List[String]]                                   = F.pure(List.empty)
    override def delete(tables: Set[TableDef]): F[Throwable, Unit]                                             = F.unit
    override def deletePrefixed[P: TablePrefix](prefix: P)(tables: Set[TableDef]): F[Throwable, Unit]          = F.unit
    override def backup(tables: Set[TableDef]): F[Throwable, Unit]                                             = F.unit
    override def backupPrefixed[P: TablePrefix](prefix: P)(tables: Set[TableDef]): F[Throwable, Unit]          = F.unit
  }

  // For test purposes
  final class Memo[F[+_, +_]: Async2: Temporal2](
    logger: LogIO2[F],
    interpreter: DynamoInterpreter[F],
  ) extends DynamoTablesService.Impl[F](logger, interpreter) {
    val prefixedTables: mutable.Set[String] = mutable.Set.empty[String]

    override def createPrefixed[P: TablePrefix](prefix: P)(tables: Set[TableDef]): F[Throwable, Unit] = {
      prefixedTables ++= tables.map(_.table.withPrefix(prefix).fullName)
      super.createPrefixed(prefix)(tables)
    }

    override def markForDeletionPrefixed[P: TablePrefix](prefix: P)(tables: Set[TableDef]): F[Throwable, Unit] = {
      prefixedTables --= tables.map(_.table.withPrefix(prefix).fullName)
      super.markForDeletionPrefixed(prefix)(tables)
    }

    override def deletePrefixed[P: TablePrefix](prefix: P)(tables: Set[TableDef]): F[Throwable, Unit] = {
      prefixedTables --= tables.map(_.table.withPrefix(prefix).fullName)
      super.deletePrefixed(prefix)(tables)
    }
  }

  sealed class Impl[F[+_, +_]: Async2: Temporal2](
    log: LogIO2[F],
    interpreter: DynamoInterpreter[F],
  ) extends DynamoTablesService[F] {

    override def create(tables: Set[TableDef]): F[Throwable, Unit] =
      create(tables, identity)

    override def createPrefixed[P: TablePrefix](prefix: P)(tables: Set[TableDef]): F[Throwable, Unit] =
      create(tables, _.withPrefix(prefix))

    override def markForDeletion(tables: Set[TableDef]): F[Throwable, Unit] =
      markForDeletion(tables, identity)

    override def markForDeletionPrefixed[P: TablePrefix](prefix: P)(tables: Set[TableDef]): F[Throwable, Unit] =
      markForDeletion(tables, _.withPrefix(prefix))

    override def delete(tables: Set[TableDef]): F[Throwable, Unit] =
      delete(tables, identity)

    override def deletePrefixed[P: TablePrefix](prefix: P)(tables: Set[TableDef]): F[Throwable, Unit] =
      delete(tables, _.withPrefix(prefix))

    override def backup(tables: Set[TableDef]): F[Throwable, Unit] =
      backup(tables, identity)

    override def backupPrefixed[P: TablePrefix](prefix: P)(tables: Set[TableDef]): F[Throwable, Unit] =
      backup(tables, _.withPrefix(prefix))

    override def listTables: F[Throwable, List[String]] = {
      val exec = DynamoExecution.listTables
      exec.executionStrategy(StrategyInput(exec.dynamoQuery, interpreter))
    }

    override def listTablesByRegex(regex: Regex): F[Throwable, List[String]] = {
      listTables.map(_.flatMap(regex.findFirstIn))
    }

    private[this] def create(tables: Set[TableDef], tweak: TableReference => TableReference): F[Throwable, Unit] = {
      F.parTraverseN_(5)(tables) {
        ddl =>
          val newTable = tweak(ddl.table)
          val exec     = DynamoExecution.createTable[F](newTable, ddl.ddl)
          log.info(s"Going to create ${newTable.fullName -> "table"}; ${ddl.ddl.provisioning -> "provisioning"}.") *>
          exec.executionStrategy(StrategyInput(exec.dynamoQuery, interpreter))
      }
    }

    private[this] def markForDeletion(tables: Set[TableDef], tweak: TableReference => TableReference): F[Throwable, Unit] = {
      F.parTraverseN_(5)(tables) {
        ddl =>
          val newTable = tweak(ddl.table)
          (for {
            arn <- interpreter.run(newTable.describe, PartialFunction.empty).map(_.table().tableArn())
            _   <- log.info(s"Going to mark table for deletion ${newTable.fullName -> "table"}, $arn")
            _   <- interpreter.run(newTable.markForDeletion(arn), PartialFunction.empty)
          } yield ()).catchAll(error => log.error(s"Error when mark table for deletion $error $ddl")) // ignore errors
      }
    }

    private[this] def delete(tables: Set[TableDef], tweak: TableReference => TableReference): F[Throwable, Unit] = {
      F.parTraverseN_(5)(tables) {
        ddl =>
          val newTable = tweak(ddl.table)
          (for {
            _ <- log.info(s"Going to delete ${newTable.fullName -> "table"}.")
            _ <- interpreter.run(DeleteTable(newTable).toQuery, PartialFunction.empty)
          } yield ()).catchAll(error => log.error(s"Error when deleting table $error $ddl")) // ignore errors
      }
    }

    private[this] def backup(tables: Set[TableDef], tweak: TableReference => TableReference): F[Throwable, Unit] = {
      F.parTraverseN_(5)(tables) {
        ddl =>
          val newTable = tweak(ddl.table)
          for {
            _ <- log.info(s"Going to backup ${newTable.fullName -> "table"}.")
            _ <- interpreter.run(CreateBackup(newTable).toQuery, PartialFunction.empty)
          } yield ()
      }
    }
  }

}
