package net.playq.aws.tagging.modules

import izumi.distage.config.ConfigModuleDef
import izumi.distage.model.definition.ModuleDef
import izumi.distage.model.definition.StandardAxis.Mode
import net.playq.aws.tagging.AwsNameSpace

object AwsTagsModule extends ModuleDef with ConfigModuleDef {
  private[this] val globalTestAwsNamespace = AwsNameSpace.newTestNameSpace()

  make[AwsNameSpace]
    .tagged(Mode.Prod)
    .fromConfig("aws")

  make[AwsNameSpace]
    .tagged(Mode.Test)
    .fromValue(globalTestAwsNamespace)
}
