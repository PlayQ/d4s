package d4s

import d4s.models.table.TableDef
import izumi.distage.model.definition.DIResource
import izumi.functional.bio.BIOAsync
import logstage.LogBIO

final class DynamoDDLService[F[+_, +_]: BIOAsync: LogBIO](
  instances: Set[TableDef],
  tablesManager: DynamoTablesService[F],
) extends DIResource.SelfNoClose[F[Throwable, ?], DynamoDDLService[F]] {

  override def acquire: F[Throwable, Unit] = {
    for {
      _ <- LogBIO[F].info(s"Dynamo performing up scripts...")
      _ <- tablesManager.create(instances)
      _ <- LogBIO[F].info("Upping finished")
    } yield ()
  }

  def down(): F[Throwable, Unit] = {
    for {
      _ <- LogBIO[F].info(s"performing down scripts...")
      _ <- tablesManager.delete(instances)
      _ <- LogBIO[F].info("downing finished")
    } yield ()
  }
}
