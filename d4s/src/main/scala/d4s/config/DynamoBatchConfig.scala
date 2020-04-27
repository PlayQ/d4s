package d4s.config

import scala.concurrent.duration.FiniteDuration

final case class DynamoBatchConfig(
  unprocessedBatchSleep: Option[FiniteDuration],
  writeBatchSize: Int,
  getBatchSize: Int,
)
