package d4s

import d4s.DynamoConnector.{DynamoException, WithDynamoConnector}
import d4s.metrics.{MacroMetricDynamoMeter, MacroMetricDynamoTimer}
import d4s.models.DynamoExecution
import d4s.models.query.DynamoRequest
import fs2.Stream
import zio.{IO, ZIO}

object d4z extends DynamoConnector[ZIO[WithDynamoConnector[IO], +?, +?]] {

  override def runUnrecorded[DR <: DynamoRequest, A](q: DynamoExecution[DR, _, A]): ZIO[WithDynamoConnector[IO], Throwable, A] = {
    ZIO.accessM(_.connector.runUnrecorded(q))
  }

  override def runUnrecorded[DR <: DynamoRequest, A](q: DynamoExecution.Streamed[DR, _, A]): Stream[ZIO[WithDynamoConnector[IO], Throwable, ?], A] = {
    Stream.force[ZIO[WithDynamoConnector[IO], Throwable, ?], A] {
      ZIO.access[WithDynamoConnector[IO]](_.connector.runUnrecorded(q))
    }
  }

  override def run[DR <: DynamoRequest, Dec, A](label: String)(
    q: DynamoExecution[DR, Dec, A]
  )(implicit macroTimeSaver: MacroMetricDynamoTimer[label.type], macroMeterSaver: MacroMetricDynamoMeter[label.type]): ZIO[WithDynamoConnector[IO], DynamoException, A] =
    ZIO.accessM(_.connector.run(label)(q))

  override def runStreamed[DR <: DynamoRequest, Dec, A](label: String)(
    q: DynamoExecution.Streamed[DR, Dec, A]
  )(implicit macroTimeSaver: MacroMetricDynamoTimer[label.type],
    macroMeterSaver: MacroMetricDynamoMeter[label.type]): Stream[ZIO[WithDynamoConnector[IO], DynamoException, ?], A] = {

    Stream.force[ZIO[WithDynamoConnector[IO], DynamoException, ?], A] {
      ZIO.access[WithDynamoConnector[IO]](_.connector.runStreamed(label)(q))
    }
  }

}
