#!/bin/sh
coursier launch com.lihaoyi:ammonite_2.13.0:1.6.9 --fork -M ammonite.Main -- sbtgen.sc $*
exit
!#

import java.nio.file.{FileSystems, Files}

import $ivy.`io.7mind.izumi.sbt::sbtgen:0.0.52`, izumi.sbtgen._, izumi.sbtgen.model._, izumi.sbtgen.model.LibSetting.Exclusion
import ProjectBuilder.ProjectDeps._

val settings = GlobalSettings(
  groupId = "net.playq",
  sbtVersion = None,
)

@main
def entrypoint(args: String*) = Entrypoint.main(ProjectBuilder.root, settings, Seq("-o", ".") ++ args)

object Targets {
  final val scala212 = ScalaVersion(getScalaVersion("212"))
  final val scala213 = ScalaVersion(getScalaVersion("213"))

  private def getScalaVersion(v: String): String = {
    s"""scala_$v.*"(.*)"""".r
      .findFirstMatchIn(
        Files.readString(FileSystems.getDefault.getPath("project/Versions.scala"))
      ).map(_.group(1)).getOrElse(throw new RuntimeException(s"Couldn't get `scala_$v` version from project/Versions.scala"))
  }
  val targetScala = Seq(scala213, scala212)
  private val jvmPlatform = PlatformEnv(
    platform = Platform.Jvm,
    language = targetScala,
    settings = Seq(
      "scalacOptions" ++= Seq(
        SettingKey(Some(scala212), None) := Defaults.Scala212Options,
        SettingKey(Some(scala213), None) := Defaults.Scala213Options,
        SettingKey.Default := Const.EmptySeq,
      ),
      "scalacOptions" ++= Seq(
        SettingKey(Some(scala212), Some(true)) := Seq(
          "-opt:l:inline",
          "-opt-inline-from:izumi.**",
          "-opt-inline-from:net.playq.**",
        ),
        SettingKey(Some(scala213), Some(true)) := Seq(
          "-opt:l:inline",
          "-opt-inline-from:izumi.**",
          "-opt-inline-from:net.playq.**",
        ),
        SettingKey.Default := Const.EmptySeq
      ),
    )
  )

  final val jvm = Seq(jvmPlatform)
}


object ProjectBuilder {

  object ProjectDeps {
    final val distage_framework      = Library("io.7mind.izumi", "distage-framework", Version.VExpr("V.izumi_version"), LibraryType.Auto)
    final val distage_plugins        = Library("io.7mind.izumi", "distage-extension-plugins", Version.VExpr("V.izumi_version"), LibraryType.Auto)
    final val distage_config         = Library("io.7mind.izumi", "distage-extension-config", Version.VExpr("V.izumi_version"), LibraryType.Auto)
    final val distage_testkit        = Library("io.7mind.izumi", "distage-testkit-scalatest", Version.VExpr("V.izumi_version"), LibraryType.Auto)
    final val distage_docker         = Library("io.7mind.izumi", "distage-framework-docker", Version.VExpr("V.izumi_version"), LibraryType.Auto)

    final val circe = Seq(
      Library("io.circe", "circe-core", Version.VExpr("V.circe"), LibraryType.Auto),
      Library("io.circe", "circe-generic", Version.VExpr("V.circe"), LibraryType.Auto),
      Library("io.circe", "circe-generic-extras", Version.VExpr("V.circe_generic_extras"), LibraryType.Auto),
      Library("io.circe", "circe-parser", Version.VExpr("V.circe"), LibraryType.Auto),
      Library("io.circe", "circe-literal", Version.VExpr("V.circe"), LibraryType.Auto),
      Library("io.circe", "circe-derivation", Version.VExpr("V.circe_derivation"), LibraryType.Auto),
    )

    final val magnolia = Library("com.propensive", "magnolia", Version.VExpr("V.magnolia_version"), LibraryType.Auto)

    final val scala_reflect = Library("org.scala-lang", "scala-reflect", Version.VExpr("scalaVersion.value"), LibraryType.Invariant)

    final val logstage_rendering_circe = Library("io.7mind.izumi", "logstage-rendering-circe", Version.VExpr("V.izumi_version"), LibraryType.Auto)
    final val logstage_adapter_slf4j   = Library("io.7mind.izumi", "logstage-adapter-slf4j", Version.VExpr("V.izumi_version"), LibraryType.Auto)
    final val logstage_core            = Library("io.7mind.izumi", "logstage-core", Version.VExpr("V.izumi_version"), LibraryType.Auto)

    final val zio_core    = Library("dev.zio", "zio", Version.VExpr("V.zio"), LibraryType.Auto)
    final val zio_interop = Library("dev.zio", "zio-interop-cats", Version.VExpr("V.zio_interop_cats"), LibraryType.Auto)
    final val fs2         = Library("co.fs2", "fs2-io", Version.VExpr("V.fs2"), LibraryType.Auto)

    final val scalatest                = Library("org.scalatest", "scalatest", Version.VExpr("V.scalatest"), LibraryType.Auto)
    final val scalatestplus_scalacheck = Library("org.scalatestplus", "scalacheck-1-14", Version.VExpr("V.scalatestplus_scalacheck"), LibraryType.Auto)
    final val scalacheck_shapeless     = Library("com.github.alexarchambault", "scalacheck-shapeless_1.14", Version.VExpr("V.scalacheck_shapeless"), LibraryType.Auto)

    final val fundamentals_bio = Library("io.7mind.izumi", "fundamentals-bio", Version.VExpr("V.izumi_version"), LibraryType.Auto)

    final val aws_dynamo = Library("software.amazon.awssdk", "dynamodb", Version.VExpr("V.aws_java_sdk_2"), LibraryType.Invariant)
      .more(LibSetting.Exclusions(Seq(Exclusion("log4j", "log4j"))))

    final val aws_impl_apache = Library("software.amazon.awssdk", "apache-client", Version.VExpr("V.aws_java_sdk_2"), LibraryType.Invariant)
      .more(LibSetting.Exclusions(Seq(Exclusion("log4j", "log4j"))))

    final val projector = Library("org.typelevel", "kind-projector", Version.VExpr("V.kind_projector"), LibraryType.Invariant)
      .more(LibSetting.Raw("cross CrossVersion.full"))
  }

  object ProjectSettings {
    final val sharedImports = Seq(
      Import("IzumiPublishingPlugin.Keys._"),
      Import("IzumiPublishingPlugin.autoImport._"),
      Import("IzumiConvenienceTasksPlugin.Keys._"),
      Import("sbtrelease.ReleaseStateTransformations._"),
    )

    final val rootProjectSettings = Defaults.SharedOptions ++ Seq(
      "fork" in SettingScope.Raw("Global") := false,

      "crossScalaVersions" := "Nil".raw,
      "scalaVersion" := Targets.targetScala.head.value,
      "coverageOutputXML" in SettingScope.Raw("Global") := true,
      "coverageOutputHTML" in SettingScope.Raw("Global") := true,
      "organization" in SettingScope.Raw("Global") := "net.playq",

      "sonatypeProfileName" := "net.playq",
      "sonatypeSessionName" := """s"[sbt-sonatype] ${name.value} ${version.value} ${java.util.UUID.randomUUID}"""".raw,
      "publishTo" in SettingScope.Build :=
        """
          |(if (!isSnapshot.value) {
          |    sonatypePublishToBundle.value
          |  } else {
          |    Some(Opts.resolver.sonatypeSnapshots)
          |})
          |""".stripMargin.raw,

      "credentials" in SettingScope.Build += """Credentials(file(".secrets/credentials.sonatype-nexus.properties"))""".raw,
      "homepage" in SettingScope.Build := """Some(url("https://www.playq.com/"))""".raw,
      "licenses" in SettingScope.Build := """Seq("Apache-License" -> url("https://opensource.org/licenses/Apache-2.0"))""".raw,

      "scmInfo" in SettingScope.Build := """Some(ScmInfo(url("https://github.com/PlayQ/d4s"), "scm:git:https://github.com/PlayQ/d4s.git"))""".raw,
      "scalacOptions" in SettingScope.Build += s"""${"\"" * 3}-Xmacro-settings:scalatest-version=${Version.VExpr("V.scalatest")}${"\"" * 3}""".raw,

      "releaseProcess" := """Seq[ReleaseStep](
                            |  checkSnapshotDependencies,
                            |  inquireVersions,
                            |  runClean,
                            |  runTest,
                            |  setReleaseVersion,
                            |  commitReleaseVersion,
                            |  tagRelease,
                            |  //publishArtifacts,
                            |  setNextVersion,
                            |  commitNextVersion,
                            |  pushChanges
                            |)""".stripMargin.raw,

      // sbt 1.3.0
      "onChangedBuildSource" in SettingScope.Raw("Global") := "ReloadOnSourceChanges".raw,
    )

    final val sharedSettings = Seq(
      "scalacOptions" in SettingScope.Compile += """s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}"""".raw,
      "scalacOptions" in SettingScope.Test += """s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}"""".raw,
      "scalacOptions" in SettingScope.Test += """s"-Xmacro-settings:metricsDir=${(classDirectory in Test).value}"""".raw,

      "scalacOptions" in SettingScope.Compile += """s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}"""".raw,
      "scalacOptions" in SettingScope.Test += """s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}"""".raw,
      "scalacOptions" in SettingScope.Test += """s"-Xmacro-settings:metricsRole=${(name in Test).value};${(moduleName in Test).value}"""".raw,

      "testOptions" in SettingScope.Test += """Tests.Argument("-oDF")""".raw,
      "logBuffered" in SettingScope.Test := true,

      "resolvers" += "DefaultMavenRepository".raw,
    )

    final val crossScalaSources = Seq(
      "unmanagedSourceDirectories" in SettingScope.Compile :=
        """(unmanagedSourceDirectories in Compile).value.flatMap {
          |  dir =>
          |   Seq(dir, file(dir.getPath + (CrossVersion.partialVersion(scalaVersion.value) match {
          |     case Some((2, 11)) => "_2.11"
          |     case Some((2, 12)) => "_2.12"
          |     case Some((2, 13)) => "_2.13"
          |     case _             => "_3.0"
          |   })))
          |}""".stripMargin.raw,
    )
  }

  object Projects {
    final val aws_common = ArtifactId("aws-common")
    final val metrics = ArtifactId("metrics")
    final val dynamo = ArtifactId("dynamo")
    final val dynamo_test = ArtifactId("dynamo-test")
    final val dynamo_circe = ArtifactId("dynamo-circe")
  }

  final val dynamo_agg = Aggregate(
    name = ArtifactId("d4s-agg"),
    artifacts = Seq(
      Artifact(
        name    = Projects.aws_common,
        libs    = Seq(
          distage_plugins,
          distage_config,
        ).map(_ in Scope.Compile.all),
        settings = ProjectSettings.crossScalaSources,
        depends = Seq.empty,
      ),
      Artifact(
        name = Projects.metrics,
        libs = Seq(
          distage_framework,
          zio_core,
        ).map(_ in Scope.Compile.all) ++ Seq(
          scala_reflect in Scope.Provided.all,
        ),
        settings = ProjectSettings.crossScalaSources,
        depends = Seq.empty,
      ),
      Artifact(
        name = Projects.dynamo,
        libs = Seq(
          zio_core,
          zio_interop,
          fs2,
          fundamentals_bio,
          distage_framework,
          logstage_adapter_slf4j,
          aws_dynamo,
          aws_impl_apache,
          magnolia,
        ).map(_ in Scope.Compile.all)  ++ Seq(
          scala_reflect in Scope.Provided.all,
        ),
        settings = ProjectSettings.crossScalaSources,
        depends = Seq(
          Projects.aws_common,
          Projects.metrics,
        ),
      ),
      Artifact(
        name = Projects.dynamo_test,
        libs = Seq(
          distage_docker,
          distage_testkit,
        ).map(_ in Scope.Compile.all) ++ Seq(
          scalatest,
          scalatestplus_scalacheck,
          scalacheck_shapeless
        ).map(_ in Scope.Test.all),
        depends = Seq(Projects.dynamo),
      ),
      Artifact(
        name = Projects.dynamo_circe,
        libs = circe.map(_ in Scope.Compile.all) ++ Seq(
          scalatest,
          scalatestplus_scalacheck,
          scalacheck_shapeless
        ).map(_ in Scope.Test.all),
        depends = Seq(Projects.dynamo),
      )
    ),
    pathPrefix       = Seq("."),
    groups           = Set(Group("dynamo")),
    defaultPlatforms = Targets.jvm,
    sharedSettings = Seq(
      "scalacOptions" in SettingScope.Compile += "-Xmacro-settings:metricsRole=default"
    ),
  )

  final val root = Project(
    name = ArtifactId("d4s"),
    aggregates = {
      Seq(dynamo_agg)
    },
    sharedSettings = ProjectSettings.sharedSettings,
    rootSettings = ProjectSettings.rootProjectSettings,
    imports = ProjectSettings.sharedImports,
    globalLibs = Seq(
      (projector in Scope.Compile.all).copy(compilerPlugin = true),
    ),
    rootPlugins = Plugins(
      enabled = Seq(
        Plugin("IzumiPublishingPlugin"),
        Plugin("IzumiResolverPlugin"),
        Plugin("IzumiConvenienceTasksPlugin"),
        Plugin("SbtgenVerificationPlugin"),
      ),
    ),
    globalPlugins = Plugins(
      enabled = Seq(
        Plugin("IzumiPublishingPlugin"),
        Plugin("IzumiResolverPlugin"),
      ),
    ),
    appendPlugins = Defaults.SbtGenPlugins,
  )
}
