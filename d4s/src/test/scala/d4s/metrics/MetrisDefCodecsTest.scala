package d4s.metrics

import d4s.codecs.D4SAttributeEncoder
import net.playq.metrics.base.MetricDef
import org.scalatest.wordspec.AnyWordSpec

class MetrisDefCodecsTest extends AnyWordSpec {

  "MetricDef implicits are searchable without circe-core on the classpath (no more orphans)" in {
    def summonOrNull[T >: Null](implicit t: T = null): t.type = t
    assert(summonOrNull[D4SAttributeEncoder[MetricDef]] == null)
  }

}
