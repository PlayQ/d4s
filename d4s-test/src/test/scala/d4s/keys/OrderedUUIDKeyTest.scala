package d4s.keys

import java.util.UUID

import izumi.fundamentals.platform.uuid.UUIDGen
import org.scalacheck.Prop.AnyOperators
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers

final class OrderedUUIDKeyTest extends AnyWordSpec with Checkers {

  // test 10k times
  implicit val propertyConf: PropertyCheckConfiguration = {
    generatorDrivenConfig.copy(minSuccessful = 10000)
  }

  "OrderedUUIDKey has the same lexicographic ordering as the UUID's usual ordering" in check {
    uuids: List[UUID] =>
      val sortedUUIDs = uuids.sorted

      val strings       = sortedUUIDs.map(OrderedUUIDKey(_).asString)
      val sortedStrings = strings.sorted

      sortedStrings ?= strings
  }

  "TimeUUID ordering" in check {
    _: Unit =>
      val uuids = List.fill(20)(UUIDGen.getTimeUUID()).sorted
      val strs  = uuids.map(OrderedUUIDKey(_).asString)

      val sortedUUID = uuids.map(OrderedUUIDKey(_).asString)
      val sortedSTR  = strs.sorted

      sortedUUID ?= sortedSTR
  }

}
