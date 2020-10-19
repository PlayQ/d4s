package d4s.health

import d4s.DynamoClient
import izumi.functional.bio.{Exit, Panic2}

final class DynamoDBHealthChecker[F[+_, +_]: Panic2](client: DynamoClient[F]) {
  def healthCheck(): F[Throwable, Set[HealthCheckStatus]] = {
    client
      .raw(_.listTables)
      .sandboxExit.map {
        case _: Exit.Success[_] =>
          Set(HealthCheckStatus("dynamodb.session", HealthState.OK))
        case _: Exit.Error[_] =>
          Set(HealthCheckStatus("dynamodb.session", HealthState.DEFUNCT))
        case _: Exit.Termination =>
          Set(HealthCheckStatus("dynamodb.session", HealthState.UNKNOWN))
      }
  }
}
