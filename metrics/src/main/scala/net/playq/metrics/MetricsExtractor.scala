package net.playq.metrics

import java.nio.charset.StandardCharsets.UTF_8

import cats.instances.list._
import cats.syntax.foldable._
import io.github.classgraph.ClassGraph
import izumi.distage.roles.model.meta.RolesInfo
import logstage.IzLogger
import net.playq.metrics.base.MetricDef
import net.playq.metrics.macrodefs.MacroMetricSaver
import net.playq.metrics.macrodefs.MacroMetricSaver.metricsDir

import scala.jdk.CollectionConverters._

final class MetricsExtractor(rolesInfo: RolesInfo, logger: IzLogger) {

  final val allRoles = s"${rolesInfo.requiredRoleBindings.map(_.descriptor.id).mkString(";")}${MacroMetricSaver.defaultMetricRole}"

  def collectMetrics: Set[MetricDef] = {
    val scan         = new ClassGraph().scan()
    val resourceList = scan.getResourcesMatchingPattern(s"$metricsDir/.*".r.pattern)

    try {
      val (errors, rawFetched) = resourceList.asScala.toList.flatMap {
        case res if !res.getPath.endsWith("metrics.json") =>
          val filename = res.getPath
          logger.crit(s"Found a junk file with $filename in $metricsDir - filename does not end with `metrics.json`, Skipping.")
          Nil
        case res =>
          val bytes = res.load()
          new String(bytes, UTF_8).linesIterator
            .filter(_.nonEmpty)
            .map(io.circe.parser.decode[MetricDef])
      }.partitionEither(identity)

      reportErrors(errors)
      inRoles(rawFetched.toSet)
    } finally {
      resourceList.close()
      scan.close()
    }
  }

  @inline private def inRoles(metrics: Set[MetricDef]): Set[MetricDef] = metrics.filter(m => allRoles.contains(m.role))

  private def reportErrors(errors: List[io.circe.Error]): Unit = {
    if (errors.nonEmpty) {
      logger.crit(s"Couldn't read some of the metrics - $errors")
    }
  }
}
