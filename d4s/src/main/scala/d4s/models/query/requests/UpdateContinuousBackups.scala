package d4s.models.query.requests

import d4s.models.query.DynamoRequest
import d4s.models.table.TableReference
import software.amazon.awssdk.services.dynamodb.model.{PointInTimeRecoverySpecification, UpdateContinuousBackupsRequest, UpdateContinuousBackupsResponse}

final case class UpdateContinuousBackups(
  table: TableReference,
  backupEnabled: Boolean,
) extends DynamoRequest {

  override type Rq  = UpdateContinuousBackupsRequest
  override type Rsp = UpdateContinuousBackupsResponse

  override def toAmz: UpdateContinuousBackupsRequest = {
    UpdateContinuousBackupsRequest
      .builder()
      .tableName(table.fullName)
      .pointInTimeRecoverySpecification(
        PointInTimeRecoverySpecification
          .builder()
          .pointInTimeRecoveryEnabled(backupEnabled)
          .build()
      )
      .build()
  }

}
