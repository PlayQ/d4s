package d4s

import d4s.models.DynamoExecution
import d4s.models.ExecutionStrategy.StrategyInput
import d4s.models.table.{TableDef, TablePrefix, TableReference}
import izumi.functional.bio.{BIOApplicative, BIOTemporal, F}
import logstage.LogBIO

import scala.collection.mutable
import scala.util.matching.Regex

trait DynamoTablesService[F[_, _]] {
  def create(tables: Set[TableDef]): F[Throwable, Unit]
  def createPrefixed[P: TablePrefix](prefix: P)(tables: Set[TableDef]): F[Throwable, Unit]

  def delete(tables: Set[TableDef]): F[Throwable, Unit]
  def deletePrefixed[P: TablePrefix](prefix: P)(tables: Set[TableDef]): F[Throwable, Unit]

  def listTables: F[Throwable, List[String]]
  def listTablesByRegex(regex: Regex): F[Throwable, List[String]]
}

object DynamoTablesService {

  final class Empty[F[+_, +_]: BIOApplicative] extends DynamoTablesService[F] {
    override def create(tables: Set[TableDef]): F[Throwable, Unit]                                    = F.unit
    override def createPrefixed[P: TablePrefix](prefix: P)(tables: Set[TableDef]): F[Throwable, Unit] = F.unit
    override def delete(tables: Set[TableDef]): F[Throwable, Unit]                                    = F.unit
    override def deletePrefixed[P: TablePrefix](prefix: P)(tables: Set[TableDef]): F[Throwable, Unit] = F.unit
    override def listTables: F[Throwable, List[String]]                                               = F.pure(List.empty)
    override def listTablesByRegex(regex: Regex): F[Throwable, List[String]]                          = F.pure(List.empty)
  }

  final class Memo[F[+_, +_]: BIOTemporal](
    logger: LogBIO[F],
    interpreter: DynamoInterpreter[F],
  ) extends DynamoTablesService.Impl[F](logger, interpreter) {
    val prefixedTables: mutable.Set[String] = mutable.Set.empty[String]

    override def createPrefixed[P](prefix: P)(tables: Set[TableDef])(implicit evidence$7: TablePrefix[P]): F[Throwable, Unit] = {
      prefixedTables ++= tables.map(_.table.withPrefix(prefix).fullName)
      super.createPrefixed(prefix)(tables)
    }

    override def deletePrefixed[P](prefix: P)(tables: Set[TableDef])(implicit evidence$8: TablePrefix[P]): F[Throwable, Unit] = {
      prefixedTables --= tables.map(_.table.withPrefix(prefix).fullName)
      super.deletePrefixed(prefix)(tables)
    }
  }

  sealed class Impl[F[+_, +_]: BIOTemporal](
    log: LogBIO[F],
    interpreter: DynamoInterpreter[F],
  ) extends DynamoTablesService[F] {

    override def create(tables: Set[TableDef]): F[Throwable, Unit] =
      create(tables, identity)

    override def createPrefixed[P: TablePrefix](prefix: P)(tables: Set[TableDef]): F[Throwable, Unit] =
      create(tables, _.withPrefix(prefix))

    override def delete(tables: Set[TableDef]): F[Throwable, Unit] =
      delete(tables, identity)

    override def deletePrefixed[P: TablePrefix](prefix: P)(tables: Set[TableDef]): F[Throwable, Unit] =
      delete(tables, _.withPrefix(prefix))

    override def listTables: F[Throwable, List[String]] = {
      val exec = DynamoExecution.listTables
      exec.executionStrategy(StrategyInput(exec.dynamoQuery, F, interpreter))
    }

    override def listTablesByRegex(regex: Regex): F[Throwable, List[String]] = {
      listTables.map(_.flatMap(regex.findFirstIn))
    }

    private[this] def create(tables: Set[TableDef], tweak: TableReference => TableReference): F[Throwable, Unit] = {
      F.parTraverseN(5)(tables) {
          ddl =>
            val newTable = tweak(ddl.table)
            val exec     = DynamoExecution.createTable[F](newTable, ddl.ddl)
            log.info(s"Going to create ${newTable.fullName}; ${ddl.ddl.provisioning -> "provisioning"}.") *>
            exec.executionStrategy(StrategyInput(exec.dynamoQuery, F, interpreter))
        }.void
    }

    private[this] def delete(tables: Set[TableDef], tweak: TableReference => TableReference): F[Throwable, Unit] = {
      F.parTraverseN(5)(tables) {
          ddl =>
            val newTable = tweak(ddl.table)
            (for {
              arn <- interpreter.run(newTable.describe, PartialFunction.empty).map(_.table().tableArn())
              _   <- log.info(s"Going to mark table for deletion ${newTable.fullName}, $arn")
              _   <- interpreter.run(newTable.markForDeletion(arn), PartialFunction.empty)
            } yield ()).catchAll(error => log.error(s"Error when mark table for deletion $error $ddl")) // ignore errors
        }.void
    }
  }

}
