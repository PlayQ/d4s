package net.playq.aws.tagging.modules

import izumi.distage.config.ConfigModuleDef
import izumi.distage.model.definition.ModuleDef
import izumi.distage.model.definition.StandardAxis.Env
import net.playq.aws.tagging.AwsNameSpace

object AwsTagsModule extends ModuleDef with ConfigModuleDef {
  private[this] val globalTestAwsNamespace = AwsNameSpace.newTestNameSpace()

  make[AwsNameSpace]
    .tagged(Env.Prod)
    .fromConfig("aws")

  make[AwsNameSpace]
    .tagged(Env.Test)
    .fromValue(globalTestAwsNamespace)
}
