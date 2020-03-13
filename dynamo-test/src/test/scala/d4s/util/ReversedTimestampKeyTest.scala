package d4s.util

import java.time.ZonedDateTime

import d4s.env.DynamoRnd
import izumi.fundamentals.platform.time.IzTime.zonedDateTimeOrdering
import org.scalacheck.Prop._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers

final class ReversedTimestampKeyTest extends AnyWordSpec with Checkers with DynamoRnd {

  // test 10k times
  implicit val propertyConf: PropertyCheckConfiguration = {
    generatorDrivenConfig.copy(minSuccessful = 10000)
  }

  "negated timestamp has opposite lexicographic ordering to usual timestamp" in check {
    times: List[ZonedDateTime] =>
      val sortedTs = times.sorted

      val negates       = sortedTs.map(ReversedTimestampKey(_).asString)
      val sortedNegates = negates.sorted

      sortedNegates ?= negates.reverse
  }

}
