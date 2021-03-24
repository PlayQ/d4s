package d4s.config

import d4s.models.table.{HasBillingMode, HasProvisionedThroughput}
import software.amazon.awssdk.services.dynamodb.model.{BillingMode, ProvisionedThroughput}

final case class ProvisionedThroughputConfig(
  read: Long,
  write: Long,
  mode: BillingMode,
) {
  def configureThroughput[T](builder: T)(implicit ev: HasProvisionedThroughput[T]): T = {
    mode match {
      case BillingMode.PROVISIONED =>
        ev.provisionedThroughput(
          builder,
          ProvisionedThroughput
            .builder()
            .readCapacityUnits(read)
            .writeCapacityUnits(write)
            .build(),
        )
      case BillingMode.PAY_PER_REQUEST =>
        builder
      case BillingMode.UNKNOWN_TO_SDK_VERSION =>
        builder
    }
  }

  def configureBilling[T](builder: T)(implicit ev: HasBillingMode[T]): T = {
    ev.billingMode(builder, mode)
  }

  def configure[T: HasProvisionedThroughput: HasBillingMode](builder: T): T = {
    configureBilling(configureThroughput(builder))
  }
}
object ProvisionedThroughputConfig {
  final val minimal = ProvisionedThroughputConfig(1L, 1L, BillingMode.PROVISIONED)
}
