package d4s.models.table

import software.amazon.awssdk.services.dynamodb.model._

trait HasBillingMode[A] {
  def billingMode(a: A, billingMode: BillingMode): A
}

object HasBillingMode {
  @inline def apply[A: HasBillingMode]: HasBillingMode[A] = implicitly

  implicit val hasBillingModeCreateTableRequest: HasBillingMode[CreateTableRequest.Builder] = _.billingMode(_)
  implicit val hasBillingModeUpdateTableRequest: HasBillingMode[UpdateTableRequest.Builder] = _.billingMode(_)
}
