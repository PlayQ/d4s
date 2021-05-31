package d4s

import d4s.metrics.{MacroMetricDynamoMeter, MacroMetricDynamoTimer}
import d4s.models.query.DynamoRequest
import d4s.models.{DynamoException, DynamoExecution}
import fs2.Stream
import izumi.functional.bio.{F, MonadAsk3}
import izumi.reflect.Tag

class DynamoConnectorEnv[F[-_, +_, +_]: MonadAsk3](implicit tag: Tag[DynamoConnector3[F]]) extends DynamoConnector[F[HasDynamoConnector[F], + _, + _]] {
  override def runUnrecorded[DR <: DynamoRequest, A](q: DynamoExecution[DR, ?, A]): F[HasDynamoConnector[F], DynamoException, A] = {
    F.access(_.get.runUnrecorded(q))
  }

  override def runUnrecorded[DR <: DynamoRequest, A](q: DynamoExecution.Streamed[DR, ?, A]): Stream[F[HasDynamoConnector[F], DynamoException, _], A] = {
    Stream.force[F[HasDynamoConnector[F], DynamoException, _], A] {
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
  ): Stream[F[HasDynamoConnector[F], DynamoException, _], A] = {
    Stream
      .force[F[HasDynamoConnector[F], DynamoException, _], A] {
        F.askWith(_.get.runStreamed(label)(q))
      }
  }
}
