package d4s

import d4s.models.table.TableDef
import izumi.distage.model.definition.Lifecycle
import izumi.functional.bio.Monad2
import logstage.LogIO2
import logstage.LogIO2.log

final class DynamoDDLService[F[+_, +_]: Monad2: LogIO2](
  instances: Set[TableDef],
  tablesManager: DynamoTablesService[F],
) extends Lifecycle.SelfNoClose[F[Throwable, ?], DynamoDDLService[F]] {

  override def acquire: F[Throwable, Unit] = {
    for {
      _ <- log.info(s"Dynamo performing up scripts...")
      _ <- tablesManager.create(instances)
      _ <- log.info("Upping finished")
    } yield ()
  }

  def down(): F[Throwable, Unit] = {
    for {
      _ <- log.info(s"performing down scripts...")
      _ <- tablesManager.delete(instances)
      _ <- log.info("downing finished")
    } yield ()
  }
}
