package net.playq.metrics

import net.playq.metrics.base.MetricDef
import net.playq.metrics.base.MetricDef._
import org.scalacheck.ScalacheckShapeless._
import org.scalacheck.derive.MkArbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

final class MetricsCodecTest extends AnyWordSpec with ScalaCheckPropertyChecks {
  implicit val DoubleArb: Arbitrary[Double]                   = Arbitrary(Arbitrary.arbInt.arbitrary.map(_.toDouble))
  implicit val MetricCounterArb: Arbitrary[MetricCounter]     = MkArbitrary[MetricCounter].arbitrary
  implicit val MetricHistogramArb: Arbitrary[MetricHistogram] = MkArbitrary[MetricHistogram].arbitrary
  implicit val MetricTimerArb: Arbitrary[MetricTimer]         = MkArbitrary[MetricTimer].arbitrary
  implicit val MetricMeterArb: Arbitrary[MetricMeter]         = MkArbitrary[MetricMeter].arbitrary
  implicit val MetricGaugeArb: Arbitrary[MetricGauge]         = MkArbitrary[MetricGauge].arbitrary
  implicit val MetricDefArb: Arbitrary[MetricDef] = Arbitrary {
    Gen.oneOf(MetricCounterArb.arbitrary, MetricHistogramArb.arbitrary, MetricTimerArb.arbitrary, MetricMeterArb.arbitrary, MetricGaugeArb.arbitrary)
  }

  "Decode and encode metrics properly" in {
    forAll {
      metric: MetricDef =>
        val encoded = MetricDef.encode(metric)
        val decoded = MetricDef.decode(encoded)
        assert(decoded.contains(metric))
    }
  }
}
