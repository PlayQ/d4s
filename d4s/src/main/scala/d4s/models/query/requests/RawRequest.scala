package d4s.models.query.requests

import d4s.DynamoClient
import izumi.functional.bio.BIOTemporal
import d4s.models.query.DynamoRequest

trait RawRequest[Rq0, Rsp0] extends DynamoRequest {
  override type Rq  = Rq0
  override type Rsp = Rsp0

  def interpret[F[+_, +_]](rq: Rq)(implicit F: BIOTemporal[F], client: DynamoClient[F]): F[Throwable, Rsp]
}

object RawRequest {
  private[RawRequest] type UnknownF[+_, +_]

  def apply[Rq, Rsp](rq: Rq)(run: (DynamoClient[UnknownF], Rq) => UnknownF[Throwable, Rsp]): RawRequest[Rq, Rsp] = {
    new RawRequest[Rq, Rsp] {
      override def interpret[F[+_, +_]](request: Rq)(implicit F: BIOTemporal[F], client: DynamoClient[F]): F[Throwable, Rsp] = {
        // user doesn't have access to UnknownF, `run` must be completely polymorphic in F
        val runnerF = run.asInstanceOf[(DynamoClient[F], Rq) => F[Throwable, Rsp]]

        runnerF(client, request)
      }

      override val toAmz: Rq = rq
    }
  }

  def raw[Rsp](run: DynamoClient[UnknownF] => UnknownF[Throwable, Rsp]): RawRequest[Unit, Rsp] = {
    apply(())((client, _) => run(client))
  }
}
