package d4s.models.query.requests

import d4s.models.query.DynamoRequest
import d4s.models.query.DynamoRequest.WithTableReference
import d4s.models.table.TableReference
import izumi.fundamentals.platform.time.IzTime
import software.amazon.awssdk.services.dynamodb.model.{CreateBackupRequest, CreateBackupResponse}

final case class CreateBackup(
  table: TableReference
) extends DynamoRequest
  with WithTableReference[CreateBackup] {

  override type Rq  = CreateBackupRequest
  override type Rsp = CreateBackupResponse

  override def withTableReference(t: TableReference => TableReference): CreateBackup = copy(table = t(table))

  def toAmz: CreateBackupRequest = {
    val name = s"${table.fullName}_${IzTime.utcNow.toEpochSecond}"
    CreateBackupRequest
      .builder()
      .tableName(table.fullName)
      .backupName(name)
      .build()
  }
}
