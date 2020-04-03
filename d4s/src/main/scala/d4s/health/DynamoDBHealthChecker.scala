package d4s.health

import d4s.DynamoClient
import izumi.functional.bio.{BIOExit, BIOPanic}

final class DynamoDBHealthChecker[F[+_, +_]: BIOPanic](client: DynamoClient[F]) {
  def healthCheck(): F[Throwable, Set[HealthCheckStatus]] = {
    client
      .raw(_.listTables)
      .sandboxBIOExit.map {
        case _: BIOExit.Success[_] =>
          Set(HealthCheckStatus("dynamodb.session", HealthState.OK))
        case _: BIOExit.Error[_] =>
          Set(HealthCheckStatus("dynamodb.session", HealthState.DEFUNCT))
        case _: BIOExit.Termination =>
          Set(HealthCheckStatus("dynamodb.session", HealthState.UNKNOWN))
      }
  }
}
