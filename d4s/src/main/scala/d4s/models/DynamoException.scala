package d4s.models

class DynamoException private (val message: String, val cause: Throwable) extends RuntimeException(message, cause)

object DynamoException {

  def unapply(arg: DynamoException): Option[(String, Throwable)] = Some((arg.message, arg.cause))

  final case class DynamoInterpreterException(operation: String, tableName: Option[String], override val cause: Throwable)
    extends DynamoException(s"Got error during executing `$operation` for table `$tableName`. Cause: ${cause.getMessage}", cause)

  final case class DynamoQueryException private (override val message: String, override val cause: Throwable) extends DynamoException(message, cause)
  object DynamoQueryException {
    def apply(cause: Throwable): DynamoQueryException                    = from(None, cause)
    def apply(queryName: String, cause: Throwable): DynamoQueryException = from(Some(queryName), cause)
    private def from(queryName: Option[String], cause: Throwable) = {
      val mbName = queryName.fold(" ")(n => s" `$n` ")
      cause match {
        case DynamoException(message, cause) => new DynamoQueryException(s"Dynamo query" ++ mbName ++ s"failed due to error: $message", cause)
        case cause                           => new DynamoQueryException(s"Dynamo query" ++ mbName ++ s"failed due to error: ${cause.getMessage}", cause)
      }
    }
  }
}
