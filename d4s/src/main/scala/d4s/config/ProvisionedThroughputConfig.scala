package d4s.config

import software.amazon.awssdk.services.dynamodb.model._

import scala.language.reflectiveCalls

final case class ProvisionedThroughputConfig(
  read: Long,
  write: Long,
  mode: BillingMode,
) {

  def configureThroughput[T](builder: T)(implicit ev0: T <:< { def provisionedThroughput(provisionedThroughput: ProvisionedThroughput): T }): T = {
    mode match {
      case BillingMode.PROVISIONED =>
        builder
          .provisionedThroughput(
            ProvisionedThroughput
              .builder()
              .readCapacityUnits(read)
              .writeCapacityUnits(write)
              .build()
          )
      case BillingMode.PAY_PER_REQUEST =>
        builder
      case BillingMode.UNKNOWN_TO_SDK_VERSION =>
        builder
    }
  }

  def configureBilling[T](builder: T)(implicit ev0: T <:< { def billingMode(billingMode: BillingMode): T }): T = {
    builder.billingMode(mode)
  }

  def configure[T](builder: T)(implicit
                               ev0: T <:< { def provisionedThroughput(provisionedThroughput: ProvisionedThroughput): T },
                               ev1: T <:< { def billingMode(billingMode: BillingMode): T }): T = {
    configureBilling(configureThroughput(builder))
  }

}
object ProvisionedThroughputConfig {
  final val minimal = ProvisionedThroughputConfig(1L, 1L, BillingMode.PROVISIONED)
}
