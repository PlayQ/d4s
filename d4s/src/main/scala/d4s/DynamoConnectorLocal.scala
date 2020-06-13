package d4s

import d4s.metrics.{MacroMetricDynamoMeter, MacroMetricDynamoTimer}
import d4s.models.query.DynamoRequest
import d4s.models.{DynamoException, DynamoExecution}
import fs2.Stream
import izumi.functional.bio.{BIO3, BIOLocal, F}
import izumi.reflect.Tag

class DynamoConnectorLocal[F[-_, +_, +_]: BIO3: BIOLocal](implicit tag: Tag[DynamoConnector3[F]]) extends DynamoConnector[F[HasDynamoConnector[F], +?, +?]] {
  override def runUnrecorded[DR <: DynamoRequest, A](
    q: DynamoExecution[DR, _, A]
  ): F[HasDynamoConnector[F], DynamoException, A] = {
    F.access(_.get.runUnrecorded(q))
  }

  override def runUnrecorded[DR <: DynamoRequest, A](
    q: DynamoExecution.Streamed[DR, _, A]
  ): Stream[F[HasDynamoConnector[F], DynamoException, ?], A] = {
    Stream.force[F[HasDynamoConnector[F], DynamoException, ?], A] {
      F.askWith(_.get.runUnrecorded(q))
    }
  }

  override def run[DR <: DynamoRequest, Dec, A](
    label: String
  )(q: DynamoExecution[DR, Dec, A]
  )(implicit macroTimeSaver: MacroMetricDynamoTimer[label.type],
    macroMeterSaver: MacroMetricDynamoMeter[label.type],
  ): F[HasDynamoConnector[F], DynamoException, A] = {
    F.access(_.get.run(label)(q))
  }

  override def runStreamed[DR <: DynamoRequest, Dec, A](
    label: String
  )(q: DynamoExecution.Streamed[DR, Dec, A]
  )(implicit macroTimeSaver: MacroMetricDynamoTimer[label.type],
    macroMeterSaver: MacroMetricDynamoMeter[label.type],
  ): Stream[F[HasDynamoConnector[F], DynamoException, ?], A] = {
    Stream
      .force[F[HasDynamoConnector[F], DynamoException, ?], A] {
        F.askWith(_.get.runStreamed(label)(q))
      }
  }
}
