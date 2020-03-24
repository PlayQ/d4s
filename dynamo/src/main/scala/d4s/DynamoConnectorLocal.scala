package d4s

import d4s.DynamoConnector.DynamoException
import d4s.DynamoConnectorLocal.WithDynamoConnector
import d4s.metrics.{MacroMetricDynamoMeter, MacroMetricDynamoTimer}
import d4s.models.DynamoExecution
import d4s.models.query.DynamoRequest
import fs2.Stream
import izumi.functional.bio.{BIO3, BIOLocal, F}

import scala.language.implicitConversions

class DynamoConnectorLocal[F[-_, +_, +_]: BIO3: BIOLocal] extends DynamoConnector[F[WithDynamoConnector[F], +?, +?]] {
  override def runUnrecorded[DR <: DynamoRequest, A](
    q: DynamoExecution[DR, _, A]
  ): F[WithDynamoConnector[F], Throwable, A] = {
    F.access(_.connector.runUnrecorded(q))
  }

  override def runUnrecorded[DR <: DynamoRequest, A](
    q: DynamoExecution.Streamed[DR, _, A]
  ): Stream[F[WithDynamoConnector[F], Throwable, ?], A] = {
    Stream.force[F[WithDynamoConnector[F], Throwable, ?], A] {
      F.askWith(_.connector.runUnrecorded(q))
    }
  }

  override def run[DR <: DynamoRequest, Dec, A](label: String)(
    q: DynamoExecution[DR, Dec, A]
  )(implicit macroTimeSaver: MacroMetricDynamoTimer[label.type], macroMeterSaver: MacroMetricDynamoMeter[label.type]): F[WithDynamoConnector[F], DynamoException, A] = {
    F.access(_.connector.run(label)(q))
  }

  override def runStreamed[DR <: DynamoRequest, Dec, A](label: String)(
    q: DynamoExecution.Streamed[DR, Dec, A]
  )(implicit macroTimeSaver: MacroMetricDynamoTimer[label.type],
    macroMeterSaver: MacroMetricDynamoMeter[label.type]): Stream[F[WithDynamoConnector[F], DynamoException, ?], A] = {
    Stream
      .force[F[WithDynamoConnector[F], DynamoException, ?], A] {
        F.askWith(_.connector.runStreamed(label)(q))
      }
  }
}

object DynamoConnectorLocal {
  trait WithDynamoConnector[F[-_, +_, +_]] {
    def connector: DynamoConnector[F[Any, +?, +?]]
  }
  object WithDynamoConnector {
    def apply[F[-_, +_, +_]](conn: DynamoConnector[F[Any, +?, +?]]): WithDynamoConnector[F] = fromConnector(conn)
    implicit def fromConnector[F[-_, +_, +_]](conn: DynamoConnector[F[Any, +?, +?]]): WithDynamoConnector[F] = new WithDynamoConnector[F] {
      override def connector: DynamoConnector[F[Any, +*, +*]] = conn
    }
  }
}
