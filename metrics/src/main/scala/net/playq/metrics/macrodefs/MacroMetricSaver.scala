package net.playq.metrics.macrodefs

import java.io.{BufferedWriter, File, FileWriter}

import io.circe.syntax._
import izumi.fundamentals.platform.language.Quirks._
import net.playq.metrics.base.MetricDef

import scala.reflect.macros.blackbox

object MacroMetricSaver {

  final val metricsDir        = "META-INF/metrics"
  final val defaultMetricRole = "default"

  def getRoles(c: blackbox.Context): Set[String] = {
    val metricsRoleSet = c.settings
      .filter(_.startsWith("metricsRole="))
      .map(_.stripPrefix("metricsRole="))
      .flatMap(_.split(';'))
      .toSet
    if (metricsRoleSet.isEmpty)
      throw new IllegalArgumentException("Cannot find scalac option for metric role name, expected -Xmacro-settings:metricsRole=<role_name1;role_name2>")
    metricsRoleSet
  }

  def getConstantType[S: c.WeakTypeTag](c: blackbox.Context, className: String, discardName: String): String = {
    import c.universe._
    weakTypeOf[S] match {
      case ConstantType(Constant(s: String)) => s
      case tpe =>
        c.abort(
          c.enclosingPosition,
          s"""When materializing $className[$tpe],
             |Couldn't record metric for metric name `$tpe` - metric name should be a String Constant. It can't be an expression.
             |To disable metric recording for `$tpe`, import $discardName._ - a metric created like this will not be visible in MetricsApi
           """.stripMargin
        )
    }
  }

  def writeToFile(c: blackbox.Context, payload: List[MetricDef]): Unit = {
    val classesDir = c.settings
      .find(_.startsWith("metricsDir=")).getOrElse(
        throw new IllegalArgumentException(
          "Cannot find scalac option for managed resource directory, expected -Xmacro-settings:metricsDir=<sbt:classDirectory in Compile>"
        )
      )
    val targetDir = s"${classesDir.stripPrefix("metricsDir=")}/$metricsDir"

    val currentSourceFileName = c.enclosingPosition.source.file.name.stripSuffix(".scala")
    val fileName              = s"${currentSourceFileName}_line_${c.enclosingPosition.line}.metrics.json"
    val file                  = new File(s"$targetDir/$fileName")

    new File(targetDir).mkdirs()
    if (!file.exists()) {
      file.createNewFile()
    }
    val bw = new BufferedWriter(new FileWriter(file, true))
    try {
      val strings = payload.map(_.asJson.noSpaces).mkString("\n")
      bw.append(strings + "\n").discard()
    } finally {
      bw.close()
    }
  }
}
