package d4s.config

import scala.concurrent.duration.FiniteDuration

final case class DynamoConfig(
  private val endpointUrl: Option[String],
  private val region: Option[String],
  connectionTimeout: FiniteDuration,
  backupEnabled: Option[Boolean],
) {
  def maybeLocalUrl: Option[String] = endpointUrl.filter(_.nonEmpty)
  def getRegion: Option[String]     = region.filter(_.nonEmpty)
}
