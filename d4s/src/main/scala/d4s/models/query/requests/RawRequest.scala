package d4s.models.query.requests

import d4s.DynamoClient
import d4s.models.query.DynamoRequest
import izumi.functional.bio.Temporal2
import software.amazon.awssdk.services.dynamodb.model.DynamoDbRequest

trait RawRequest[Rq0 <: DynamoDbRequest, Rsp0] extends DynamoRequest {
  override type Rq  = Rq0
  override type Rsp = Rsp0

  def interpret[F[+_, +_]](rq: Rq)(implicit F: Temporal2[F], client: DynamoClient[F]): F[Throwable, Rsp]
}

object RawRequest {
  private[RawRequest] type UnknownF[+_, +_]

  def apply[Rq <: DynamoDbRequest, Rsp](rq: Rq)(run: (DynamoClient[UnknownF], Rq) => UnknownF[Throwable, Rsp]): RawRequest[Rq, Rsp] = {
    new RawRequest[Rq, Rsp] {
      override def interpret[F[+_, +_]](request: Rq)(implicit F: Temporal2[F], client: DynamoClient[F]): F[Throwable, Rsp] = {
        // user doesn't have access to UnknownF, `run` must be completely polymorphic in F
        val runnerF = run.asInstanceOf[(DynamoClient[F], Rq) => F[Throwable, Rsp]]

        runnerF(client, request)
      }

      override val toAmz: Rq = rq
    }
  }

  def raw[Rsp](run: DynamoClient[UnknownF] => UnknownF[Throwable, Rsp]): RawRequest[DynamoDbRequest, Rsp] = {
    apply(null: DynamoDbRequest)((client, _) => run(client))
  }
}
