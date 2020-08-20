package d4s.models.query.responses

import software.amazon.awssdk.services.dynamodb.model.{QueryResponse, ScanResponse}

trait HasScannedCount[A] {
  def count(a: A): Int
  def scannedCount(a: A): Int
}

object HasScannedCount {
  @inline def apply[A: HasScannedCount]: HasScannedCount[A] = implicitly

  def apply[A](count0: A => Integer, scannedCount0: A => Integer): HasScannedCount[A] = new HasScannedCount[A] {
    override def count(a: A): Int        = count0(a)
    override def scannedCount(a: A): Int = scannedCount0(a)
  }

  implicit val hasScannedCountQueryResponse: HasScannedCount[QueryResponse] = HasScannedCount(_.count(), _.scannedCount())
  implicit val hasScannedCountScanResponse: HasScannedCount[ScanResponse]   = HasScannedCount(_.count(), _.scannedCount())
}
