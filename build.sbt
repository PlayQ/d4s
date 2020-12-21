import IzumiPublishingPlugin.Keys._
import IzumiPublishingPlugin.autoImport._
import IzumiConvenienceTasksPlugin.Keys._
import sbtrelease.ReleaseStateTransformations._

enablePlugins(IzumiPublishingPlugin, IzumiResolverPlugin, IzumiConvenienceTasksPlugin, SbtgenVerificationPlugin)

lazy val `aws-common` = project.in(file("./aws-common"))
  .settings(
    libraryDependencies ++= Seq(
      compilerPlugin("org.typelevel" % "kind-projector" % V.kind_projector cross CrossVersion.full),
      "io.7mind.izumi" %% "distage-extension-config" % V.izumi_version
    )
  )
  .settings(
    scalaVersion := crossScalaVersions.value.head,
    crossScalaVersions := Seq(
      "2.13.3",
      "2.12.12"
    ),
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (_, "2.12.12") => Seq(
        "-Xsource:2.13",
        "-Ypartial-unification",
        "-Yno-adapted-args",
        "-Ybackend-parallelism",
        math.min(16, math.max(1, sys.runtime.availableProcessors() - 1)).toString,
        "-Xlint:adapted-args",
        "-Xlint:by-name-right-associative",
        "-Xlint:constant",
        "-Xlint:delayedinit-select",
        "-Xlint:doc-detached",
        "-Xlint:inaccessible",
        "-Xlint:infer-any",
        "-Xlint:missing-interpolator",
        "-Xlint:nullary-override",
        "-Xlint:nullary-unit",
        "-Xlint:option-implicit",
        "-Xlint:package-object-classes",
        "-Xlint:poly-implicit-overload",
        "-Xlint:private-shadow",
        "-Xlint:stars-align",
        "-Xlint:type-parameter-shadow",
        "-Xlint:unsound-match",
        "-opt-warnings:_",
        "-Ywarn-extra-implicit",
        "-Ywarn-unused:_",
        "-Ywarn-adapted-args",
        "-Ywarn-dead-code",
        "-Ywarn-inaccessible",
        "-Ywarn-infer-any",
        "-Ywarn-nullary-override",
        "-Ywarn-nullary-unit",
        "-Ywarn-numeric-widen",
        "-Ywarn-unused-import",
        "-Ywarn-value-discard",
        "-Ycache-plugin-class-loader:always",
        "-Ycache-macro-class-loader:last-modified"
      )
      case (_, "2.13.3") => Seq(
        "-Xlint:_,-eta-sam,-multiarg-infix,-byname-implicit",
        if (insideCI.value) "-Wconf:any:error" else "-Wconf:any:warning",
        "-Wconf:cat=optimizer:warning",
        "-Ybackend-parallelism",
        math.min(16, math.max(1, sys.runtime.availableProcessors() - 1)).toString,
        "-Wdead-code",
        "-Wextra-implicit",
        "-Wnumeric-widen",
        "-Woctal-literal",
        "-Wunused:_",
        "-Wvalue-discard",
        "-Ycache-plugin-class-loader:always",
        "-Ycache-macro-class-loader:last-modified"
      )
      case (_, _) => Seq.empty
    } },
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (false, "2.12.12") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:izumi.**",
        "-opt-inline-from:net.playq.**"
      )
      case (false, "2.13.3") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:izumi.**",
        "-opt-inline-from:net.playq.**"
      )
      case (_, _) => Seq.empty
    } },
    organization := "net.playq",
    unmanagedSourceDirectories in Compile += baseDirectory.value / ".jvm/src/main/scala" ,
    unmanagedResourceDirectories in Compile += baseDirectory.value / ".jvm/src/main/resources" ,
    unmanagedSourceDirectories in Test += baseDirectory.value / ".jvm/src/test/scala" ,
    unmanagedResourceDirectories in Test += baseDirectory.value / ".jvm/src/test/resources" ,
    scalacOptions in Compile += s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsDir=${(classDirectory in Test).value}",
    scalacOptions in Compile += s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsRole=${(name in Test).value};${(moduleName in Test).value}",
    testOptions in Test += Tests.Argument("-oDF"),
    logBuffered in Test := true,
    resolvers += DefaultMavenRepository,
    resolvers += Opts.resolver.sonatypeSnapshots,
    unmanagedSourceDirectories in Compile ++= (unmanagedSourceDirectories in Compile).value.flatMap {
      dir =>
       val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
       def scalaDir(s: String) = file(dir.getPath + s)
       (partialVersion match {
         case Some((2, n)) => Seq(scalaDir("_2"), scalaDir("_2." + n.toString))
         case Some((x, n)) => Seq(scalaDir("_3"), scalaDir("_" + x.toString + "." + n.toString))
         case None         => Seq.empty
       })
    },
    unmanagedSourceDirectories in Test ++= (unmanagedSourceDirectories in Test).value.flatMap {
      dir =>
       val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
       def scalaDir(s: String) = file(dir.getPath + s)
       (partialVersion match {
         case Some((2, n)) => Seq(scalaDir("_2"), scalaDir("_2." + n.toString))
         case Some((x, n)) => Seq(scalaDir("_3"), scalaDir("_" + x.toString + "." + n.toString))
         case None         => Seq.empty
       })
    },
    scalacOptions in Compile += "-Xmacro-settings:metricsRole=default"
  )
  .enablePlugins(IzumiPublishingPlugin, IzumiResolverPlugin)

lazy val `metrics` = project.in(file("./metrics"))
  .settings(
    libraryDependencies ++= Seq(
      compilerPlugin("org.typelevel" % "kind-projector" % V.kind_projector cross CrossVersion.full),
      "io.7mind.izumi" %% "distage-framework" % V.izumi_version excludeAll (ExclusionRule(organization = "io.circe")),
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
      "io.circe" %% "circe-core" % V.circe % Optional,
      "io.circe" %% "circe-generic" % V.circe % Optional,
      "io.circe" %% "circe-generic-extras" % V.circe_generic_extras % Optional,
      "io.circe" %% "circe-parser" % V.circe % Optional,
      "io.circe" %% "circe-literal" % V.circe % Optional,
      "io.circe" %% "circe-derivation" % V.circe_derivation % Optional,
      "org.scalatest" %% "scalatest" % V.scalatest % Test,
      "org.scalatestplus" %% "scalacheck-1-15" % V.scalatestplus_scalacheck % Test,
      "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % V.scalacheck_shapeless % Test
    )
  )
  .settings(
    scalaVersion := crossScalaVersions.value.head,
    crossScalaVersions := Seq(
      "2.13.3",
      "2.12.12"
    ),
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (_, "2.12.12") => Seq(
        "-Xsource:2.13",
        "-Ypartial-unification",
        "-Yno-adapted-args",
        "-Ybackend-parallelism",
        math.min(16, math.max(1, sys.runtime.availableProcessors() - 1)).toString,
        "-Xlint:adapted-args",
        "-Xlint:by-name-right-associative",
        "-Xlint:constant",
        "-Xlint:delayedinit-select",
        "-Xlint:doc-detached",
        "-Xlint:inaccessible",
        "-Xlint:infer-any",
        "-Xlint:missing-interpolator",
        "-Xlint:nullary-override",
        "-Xlint:nullary-unit",
        "-Xlint:option-implicit",
        "-Xlint:package-object-classes",
        "-Xlint:poly-implicit-overload",
        "-Xlint:private-shadow",
        "-Xlint:stars-align",
        "-Xlint:type-parameter-shadow",
        "-Xlint:unsound-match",
        "-opt-warnings:_",
        "-Ywarn-extra-implicit",
        "-Ywarn-unused:_",
        "-Ywarn-adapted-args",
        "-Ywarn-dead-code",
        "-Ywarn-inaccessible",
        "-Ywarn-infer-any",
        "-Ywarn-nullary-override",
        "-Ywarn-nullary-unit",
        "-Ywarn-numeric-widen",
        "-Ywarn-unused-import",
        "-Ywarn-value-discard",
        "-Ycache-plugin-class-loader:always",
        "-Ycache-macro-class-loader:last-modified"
      )
      case (_, "2.13.3") => Seq(
        "-Xlint:_,-eta-sam,-multiarg-infix,-byname-implicit",
        if (insideCI.value) "-Wconf:any:error" else "-Wconf:any:warning",
        "-Wconf:cat=optimizer:warning",
        "-Ybackend-parallelism",
        math.min(16, math.max(1, sys.runtime.availableProcessors() - 1)).toString,
        "-Wdead-code",
        "-Wextra-implicit",
        "-Wnumeric-widen",
        "-Woctal-literal",
        "-Wunused:_",
        "-Wvalue-discard",
        "-Ycache-plugin-class-loader:always",
        "-Ycache-macro-class-loader:last-modified"
      )
      case (_, _) => Seq.empty
    } },
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (false, "2.12.12") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:izumi.**",
        "-opt-inline-from:net.playq.**"
      )
      case (false, "2.13.3") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:izumi.**",
        "-opt-inline-from:net.playq.**"
      )
      case (_, _) => Seq.empty
    } },
    organization := "net.playq",
    unmanagedSourceDirectories in Compile += baseDirectory.value / ".jvm/src/main/scala" ,
    unmanagedResourceDirectories in Compile += baseDirectory.value / ".jvm/src/main/resources" ,
    unmanagedSourceDirectories in Test += baseDirectory.value / ".jvm/src/test/scala" ,
    unmanagedResourceDirectories in Test += baseDirectory.value / ".jvm/src/test/resources" ,
    scalacOptions in Compile += s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsDir=${(classDirectory in Test).value}",
    scalacOptions in Compile += s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsRole=${(name in Test).value};${(moduleName in Test).value}",
    testOptions in Test += Tests.Argument("-oDF"),
    logBuffered in Test := true,
    resolvers += DefaultMavenRepository,
    resolvers += Opts.resolver.sonatypeSnapshots,
    unmanagedSourceDirectories in Compile ++= (unmanagedSourceDirectories in Compile).value.flatMap {
      dir =>
       val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
       def scalaDir(s: String) = file(dir.getPath + s)
       (partialVersion match {
         case Some((2, n)) => Seq(scalaDir("_2"), scalaDir("_2." + n.toString))
         case Some((x, n)) => Seq(scalaDir("_3"), scalaDir("_" + x.toString + "." + n.toString))
         case None         => Seq.empty
       })
    },
    unmanagedSourceDirectories in Test ++= (unmanagedSourceDirectories in Test).value.flatMap {
      dir =>
       val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
       def scalaDir(s: String) = file(dir.getPath + s)
       (partialVersion match {
         case Some((2, n)) => Seq(scalaDir("_2"), scalaDir("_2." + n.toString))
         case Some((x, n)) => Seq(scalaDir("_3"), scalaDir("_" + x.toString + "." + n.toString))
         case None         => Seq.empty
       })
    },
    scalacOptions in Compile += "-Xmacro-settings:metricsRole=default"
  )
  .enablePlugins(IzumiPublishingPlugin, IzumiResolverPlugin)

lazy val `d4s` = project.in(file("./d4s"))
  .dependsOn(
    `aws-common` % "test->compile;compile->compile",
    `metrics` % "test->compile;compile->compile"
  )
  .settings(
    libraryDependencies ++= Seq(
      compilerPlugin("org.typelevel" % "kind-projector" % V.kind_projector cross CrossVersion.full),
      "org.typelevel" %% "cats-effect" % V.cats_effect,
      "dev.zio" %% "zio" % V.zio,
      "co.fs2" %% "fs2-io" % V.fs2,
      "io.7mind.izumi" %% "fundamentals-bio" % V.izumi_version,
      "software.amazon.awssdk" % "dynamodb" % V.aws_java_sdk_2 exclude ("log4j", "log4j"),
      "software.amazon.awssdk" % "apache-client" % V.aws_java_sdk_2 exclude ("log4j", "log4j"),
      "com.propensive" %% "magnolia" % V.magnolia_version,
      "io.7mind.izumi" %% "distage-extension-plugins" % V.izumi_version,
      "org.scala-lang.modules" %% "scala-collection-compat" % V.scala_collection_compat,
      "org.scalatest" %% "scalatest" % V.scalatest % Test,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
    )
  )
  .settings(
    scalaVersion := crossScalaVersions.value.head,
    crossScalaVersions := Seq(
      "2.13.3",
      "2.12.12"
    ),
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (_, "2.12.12") => Seq(
        "-Xsource:2.13",
        "-Ypartial-unification",
        "-Yno-adapted-args",
        "-Ybackend-parallelism",
        math.min(16, math.max(1, sys.runtime.availableProcessors() - 1)).toString,
        "-Xlint:adapted-args",
        "-Xlint:by-name-right-associative",
        "-Xlint:constant",
        "-Xlint:delayedinit-select",
        "-Xlint:doc-detached",
        "-Xlint:inaccessible",
        "-Xlint:infer-any",
        "-Xlint:missing-interpolator",
        "-Xlint:nullary-override",
        "-Xlint:nullary-unit",
        "-Xlint:option-implicit",
        "-Xlint:package-object-classes",
        "-Xlint:poly-implicit-overload",
        "-Xlint:private-shadow",
        "-Xlint:stars-align",
        "-Xlint:type-parameter-shadow",
        "-Xlint:unsound-match",
        "-opt-warnings:_",
        "-Ywarn-extra-implicit",
        "-Ywarn-unused:_",
        "-Ywarn-adapted-args",
        "-Ywarn-dead-code",
        "-Ywarn-inaccessible",
        "-Ywarn-infer-any",
        "-Ywarn-nullary-override",
        "-Ywarn-nullary-unit",
        "-Ywarn-numeric-widen",
        "-Ywarn-unused-import",
        "-Ywarn-value-discard",
        "-Ycache-plugin-class-loader:always",
        "-Ycache-macro-class-loader:last-modified"
      )
      case (_, "2.13.3") => Seq(
        "-Xlint:_,-eta-sam,-multiarg-infix,-byname-implicit",
        if (insideCI.value) "-Wconf:any:error" else "-Wconf:any:warning",
        "-Wconf:cat=optimizer:warning",
        "-Ybackend-parallelism",
        math.min(16, math.max(1, sys.runtime.availableProcessors() - 1)).toString,
        "-Wdead-code",
        "-Wextra-implicit",
        "-Wnumeric-widen",
        "-Woctal-literal",
        "-Wunused:_",
        "-Wvalue-discard",
        "-Ycache-plugin-class-loader:always",
        "-Ycache-macro-class-loader:last-modified"
      )
      case (_, _) => Seq.empty
    } },
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (false, "2.12.12") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:izumi.**",
        "-opt-inline-from:net.playq.**"
      )
      case (false, "2.13.3") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:izumi.**",
        "-opt-inline-from:net.playq.**"
      )
      case (_, _) => Seq.empty
    } },
    organization := "net.playq",
    unmanagedSourceDirectories in Compile += baseDirectory.value / ".jvm/src/main/scala" ,
    unmanagedResourceDirectories in Compile += baseDirectory.value / ".jvm/src/main/resources" ,
    unmanagedSourceDirectories in Test += baseDirectory.value / ".jvm/src/test/scala" ,
    unmanagedResourceDirectories in Test += baseDirectory.value / ".jvm/src/test/resources" ,
    scalacOptions in Compile += s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsDir=${(classDirectory in Test).value}",
    scalacOptions in Compile += s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsRole=${(name in Test).value};${(moduleName in Test).value}",
    testOptions in Test += Tests.Argument("-oDF"),
    logBuffered in Test := true,
    resolvers += DefaultMavenRepository,
    resolvers += Opts.resolver.sonatypeSnapshots,
    unmanagedSourceDirectories in Compile ++= (unmanagedSourceDirectories in Compile).value.flatMap {
      dir =>
       val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
       def scalaDir(s: String) = file(dir.getPath + s)
       (partialVersion match {
         case Some((2, n)) => Seq(scalaDir("_2"), scalaDir("_2." + n.toString))
         case Some((x, n)) => Seq(scalaDir("_3"), scalaDir("_" + x.toString + "." + n.toString))
         case None         => Seq.empty
       })
    },
    unmanagedSourceDirectories in Test ++= (unmanagedSourceDirectories in Test).value.flatMap {
      dir =>
       val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
       def scalaDir(s: String) = file(dir.getPath + s)
       (partialVersion match {
         case Some((2, n)) => Seq(scalaDir("_2"), scalaDir("_2." + n.toString))
         case Some((x, n)) => Seq(scalaDir("_3"), scalaDir("_" + x.toString + "." + n.toString))
         case None         => Seq.empty
       })
    },
    scalacOptions in Compile += "-Xmacro-settings:metricsRole=default"
  )
  .enablePlugins(IzumiPublishingPlugin, IzumiResolverPlugin)

lazy val `d4s-test` = project.in(file("./d4s-test"))
  .dependsOn(
    `d4s` % "test->compile;compile->compile"
  )
  .settings(
    libraryDependencies ++= Seq(
      compilerPlugin("org.typelevel" % "kind-projector" % V.kind_projector cross CrossVersion.full),
      "io.7mind.izumi" %% "distage-framework-docker" % V.izumi_version,
      "io.7mind.izumi" %% "distage-testkit-scalatest" % V.izumi_version,
      "dev.zio" %% "zio-interop-cats" % V.zio_interop_cats % Test,
      "org.scalatest" %% "scalatest" % V.scalatest % Test,
      "org.scalatestplus" %% "scalacheck-1-15" % V.scalatestplus_scalacheck % Test,
      "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % V.scalacheck_shapeless % Test
    )
  )
  .settings(
    scalaVersion := crossScalaVersions.value.head,
    crossScalaVersions := Seq(
      "2.13.3",
      "2.12.12"
    ),
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (_, "2.12.12") => Seq(
        "-Xsource:2.13",
        "-Ypartial-unification",
        "-Yno-adapted-args",
        "-Ybackend-parallelism",
        math.min(16, math.max(1, sys.runtime.availableProcessors() - 1)).toString,
        "-Xlint:adapted-args",
        "-Xlint:by-name-right-associative",
        "-Xlint:constant",
        "-Xlint:delayedinit-select",
        "-Xlint:doc-detached",
        "-Xlint:inaccessible",
        "-Xlint:infer-any",
        "-Xlint:missing-interpolator",
        "-Xlint:nullary-override",
        "-Xlint:nullary-unit",
        "-Xlint:option-implicit",
        "-Xlint:package-object-classes",
        "-Xlint:poly-implicit-overload",
        "-Xlint:private-shadow",
        "-Xlint:stars-align",
        "-Xlint:type-parameter-shadow",
        "-Xlint:unsound-match",
        "-opt-warnings:_",
        "-Ywarn-extra-implicit",
        "-Ywarn-unused:_",
        "-Ywarn-adapted-args",
        "-Ywarn-dead-code",
        "-Ywarn-inaccessible",
        "-Ywarn-infer-any",
        "-Ywarn-nullary-override",
        "-Ywarn-nullary-unit",
        "-Ywarn-numeric-widen",
        "-Ywarn-unused-import",
        "-Ywarn-value-discard",
        "-Ycache-plugin-class-loader:always",
        "-Ycache-macro-class-loader:last-modified"
      )
      case (_, "2.13.3") => Seq(
        "-Xlint:_,-eta-sam,-multiarg-infix,-byname-implicit",
        if (insideCI.value) "-Wconf:any:error" else "-Wconf:any:warning",
        "-Wconf:cat=optimizer:warning",
        "-Ybackend-parallelism",
        math.min(16, math.max(1, sys.runtime.availableProcessors() - 1)).toString,
        "-Wdead-code",
        "-Wextra-implicit",
        "-Wnumeric-widen",
        "-Woctal-literal",
        "-Wunused:_",
        "-Wvalue-discard",
        "-Ycache-plugin-class-loader:always",
        "-Ycache-macro-class-loader:last-modified"
      )
      case (_, _) => Seq.empty
    } },
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (false, "2.12.12") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:izumi.**",
        "-opt-inline-from:net.playq.**"
      )
      case (false, "2.13.3") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:izumi.**",
        "-opt-inline-from:net.playq.**"
      )
      case (_, _) => Seq.empty
    } },
    organization := "net.playq",
    unmanagedSourceDirectories in Compile += baseDirectory.value / ".jvm/src/main/scala" ,
    unmanagedResourceDirectories in Compile += baseDirectory.value / ".jvm/src/main/resources" ,
    unmanagedSourceDirectories in Test += baseDirectory.value / ".jvm/src/test/scala" ,
    unmanagedResourceDirectories in Test += baseDirectory.value / ".jvm/src/test/resources" ,
    scalacOptions in Compile += s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsDir=${(classDirectory in Test).value}",
    scalacOptions in Compile += s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsRole=${(name in Test).value};${(moduleName in Test).value}",
    testOptions in Test += Tests.Argument("-oDF"),
    logBuffered in Test := true,
    resolvers += DefaultMavenRepository,
    resolvers += Opts.resolver.sonatypeSnapshots,
    unmanagedSourceDirectories in Compile ++= (unmanagedSourceDirectories in Compile).value.flatMap {
      dir =>
       val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
       def scalaDir(s: String) = file(dir.getPath + s)
       (partialVersion match {
         case Some((2, n)) => Seq(scalaDir("_2"), scalaDir("_2." + n.toString))
         case Some((x, n)) => Seq(scalaDir("_3"), scalaDir("_" + x.toString + "." + n.toString))
         case None         => Seq.empty
       })
    },
    unmanagedSourceDirectories in Test ++= (unmanagedSourceDirectories in Test).value.flatMap {
      dir =>
       val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
       def scalaDir(s: String) = file(dir.getPath + s)
       (partialVersion match {
         case Some((2, n)) => Seq(scalaDir("_2"), scalaDir("_2." + n.toString))
         case Some((x, n)) => Seq(scalaDir("_3"), scalaDir("_" + x.toString + "." + n.toString))
         case None         => Seq.empty
       })
    },
    scalacOptions in Compile += "-Xmacro-settings:metricsRole=default"
  )
  .enablePlugins(IzumiPublishingPlugin, IzumiResolverPlugin)

lazy val `d4s-circe` = project.in(file("./d4s-circe"))
  .dependsOn(
    `d4s` % "test->compile;compile->compile"
  )
  .settings(
    libraryDependencies ++= Seq(
      compilerPlugin("org.typelevel" % "kind-projector" % V.kind_projector cross CrossVersion.full),
      "io.circe" %% "circe-core" % V.circe,
      "io.circe" %% "circe-generic" % V.circe,
      "io.circe" %% "circe-generic-extras" % V.circe_generic_extras,
      "io.circe" %% "circe-parser" % V.circe,
      "io.circe" %% "circe-literal" % V.circe,
      "io.circe" %% "circe-derivation" % V.circe_derivation,
      "org.scalatest" %% "scalatest" % V.scalatest % Test,
      "org.scalatestplus" %% "scalacheck-1-15" % V.scalatestplus_scalacheck % Test,
      "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % V.scalacheck_shapeless % Test
    )
  )
  .settings(
    scalaVersion := crossScalaVersions.value.head,
    crossScalaVersions := Seq(
      "2.13.3",
      "2.12.12"
    ),
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (_, "2.12.12") => Seq(
        "-Xsource:2.13",
        "-Ypartial-unification",
        "-Yno-adapted-args",
        "-Ybackend-parallelism",
        math.min(16, math.max(1, sys.runtime.availableProcessors() - 1)).toString,
        "-Xlint:adapted-args",
        "-Xlint:by-name-right-associative",
        "-Xlint:constant",
        "-Xlint:delayedinit-select",
        "-Xlint:doc-detached",
        "-Xlint:inaccessible",
        "-Xlint:infer-any",
        "-Xlint:missing-interpolator",
        "-Xlint:nullary-override",
        "-Xlint:nullary-unit",
        "-Xlint:option-implicit",
        "-Xlint:package-object-classes",
        "-Xlint:poly-implicit-overload",
        "-Xlint:private-shadow",
        "-Xlint:stars-align",
        "-Xlint:type-parameter-shadow",
        "-Xlint:unsound-match",
        "-opt-warnings:_",
        "-Ywarn-extra-implicit",
        "-Ywarn-unused:_",
        "-Ywarn-adapted-args",
        "-Ywarn-dead-code",
        "-Ywarn-inaccessible",
        "-Ywarn-infer-any",
        "-Ywarn-nullary-override",
        "-Ywarn-nullary-unit",
        "-Ywarn-numeric-widen",
        "-Ywarn-unused-import",
        "-Ywarn-value-discard",
        "-Ycache-plugin-class-loader:always",
        "-Ycache-macro-class-loader:last-modified"
      )
      case (_, "2.13.3") => Seq(
        "-Xlint:_,-eta-sam,-multiarg-infix,-byname-implicit",
        if (insideCI.value) "-Wconf:any:error" else "-Wconf:any:warning",
        "-Wconf:cat=optimizer:warning",
        "-Ybackend-parallelism",
        math.min(16, math.max(1, sys.runtime.availableProcessors() - 1)).toString,
        "-Wdead-code",
        "-Wextra-implicit",
        "-Wnumeric-widen",
        "-Woctal-literal",
        "-Wunused:_",
        "-Wvalue-discard",
        "-Ycache-plugin-class-loader:always",
        "-Ycache-macro-class-loader:last-modified"
      )
      case (_, _) => Seq.empty
    } },
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (false, "2.12.12") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:izumi.**",
        "-opt-inline-from:net.playq.**"
      )
      case (false, "2.13.3") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:izumi.**",
        "-opt-inline-from:net.playq.**"
      )
      case (_, _) => Seq.empty
    } },
    organization := "net.playq",
    unmanagedSourceDirectories in Compile += baseDirectory.value / ".jvm/src/main/scala" ,
    unmanagedResourceDirectories in Compile += baseDirectory.value / ".jvm/src/main/resources" ,
    unmanagedSourceDirectories in Test += baseDirectory.value / ".jvm/src/test/scala" ,
    unmanagedResourceDirectories in Test += baseDirectory.value / ".jvm/src/test/resources" ,
    scalacOptions in Compile += s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsDir=${(classDirectory in Test).value}",
    scalacOptions in Compile += s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsRole=${(name in Test).value};${(moduleName in Test).value}",
    testOptions in Test += Tests.Argument("-oDF"),
    logBuffered in Test := true,
    resolvers += DefaultMavenRepository,
    resolvers += Opts.resolver.sonatypeSnapshots,
    unmanagedSourceDirectories in Compile ++= (unmanagedSourceDirectories in Compile).value.flatMap {
      dir =>
       val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
       def scalaDir(s: String) = file(dir.getPath + s)
       (partialVersion match {
         case Some((2, n)) => Seq(scalaDir("_2"), scalaDir("_2." + n.toString))
         case Some((x, n)) => Seq(scalaDir("_3"), scalaDir("_" + x.toString + "." + n.toString))
         case None         => Seq.empty
       })
    },
    unmanagedSourceDirectories in Test ++= (unmanagedSourceDirectories in Test).value.flatMap {
      dir =>
       val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
       def scalaDir(s: String) = file(dir.getPath + s)
       (partialVersion match {
         case Some((2, n)) => Seq(scalaDir("_2"), scalaDir("_2." + n.toString))
         case Some((x, n)) => Seq(scalaDir("_3"), scalaDir("_" + x.toString + "." + n.toString))
         case None         => Seq.empty
       })
    },
    scalacOptions in Compile += "-Xmacro-settings:metricsRole=default"
  )
  .enablePlugins(IzumiPublishingPlugin, IzumiResolverPlugin)

lazy val `d4s-agg` = (project in file(".agg/.-d4s-agg"))
  .settings(
    skip in publish := true,
    crossScalaVersions := Nil
  )
  .enablePlugins(IzumiPublishingPlugin, IzumiResolverPlugin)
  .aggregate(
    `aws-common`,
    `metrics`,
    `d4s`,
    `d4s-test`,
    `d4s-circe`
  )

lazy val `d4s-agg-jvm` = (project in file(".agg/.-d4s-agg-jvm"))
  .settings(
    skip in publish := true,
    crossScalaVersions := Nil
  )
  .aggregate(
    `aws-common`,
    `metrics`,
    `d4s`,
    `d4s-test`,
    `d4s-circe`
  )

lazy val `d4s-root-jvm` = (project in file(".agg/.agg-jvm"))
  .settings(
    skip in publish := true,
    crossScalaVersions := Nil
  )
  .aggregate(
    `d4s-agg-jvm`
  )

lazy val `d4s-root` = (project in file("."))
  .settings(
    skip in publish := true,
    publishMavenStyle in ThisBuild := true,
    scalacOptions in ThisBuild ++= Seq(
      "-encoding",
      "UTF-8",
      "-target:jvm-1.8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-language:higherKinds",
      "-explaintypes"
    ),
    javacOptions in ThisBuild ++= Seq(
      "-encoding",
      "UTF-8",
      "-source",
      "1.8",
      "-target",
      "1.8",
      "-deprecation",
      "-parameters",
      "-Xlint:all",
      "-XDignore.symbol.file"
    ),
    scalacOptions in ThisBuild ++= Seq(
      s"-Xmacro-settings:sbt-version=${sbtVersion.value}",
      s"-Xmacro-settings:git-repo-clean=${com.typesafe.sbt.SbtGit.GitKeys.gitUncommittedChanges.value}",
      s"-Xmacro-settings:git-branch=${com.typesafe.sbt.SbtGit.GitKeys.gitCurrentBranch.value}",
      s"-Xmacro-settings:git-described-version=${com.typesafe.sbt.SbtGit.GitKeys.gitDescribedVersion.value.getOrElse("")}",
      s"-Xmacro-settings:git-head-commit=${com.typesafe.sbt.SbtGit.GitKeys.gitHeadCommit.value.getOrElse("")}"
    ),
    fork in Global := false,
    crossScalaVersions := Nil,
    scalaVersion := "2.13.3",
    coverageOutputXML in Global := true,
    coverageOutputHTML in Global := true,
    organization in Global := "net.playq",
    sonatypeProfileName := "net.playq",
    sonatypeSessionName := s"[sbt-sonatype] ${name.value} ${version.value} ${java.util.UUID.randomUUID}",
    publishTo in ThisBuild := 
    (if (!isSnapshot.value) {
        sonatypePublishToBundle.value
      } else {
        Some(Opts.resolver.sonatypeSnapshots)
    })
    ,
    credentials in ThisBuild += Credentials(file(".secrets/credentials.sonatype-nexus.properties")),
    homepage in ThisBuild := Some(url("https://www.playq.com/")),
    licenses in ThisBuild := Seq("Apache-License" -> url("https://opensource.org/licenses/Apache-2.0")),
    developers in ThisBuild := List(
              Developer(id = "playq", name = "PlayQ", url = url("https://github.com/PlayQ"), email = "platform-team@playq.net"),
            ),
    scmInfo in ThisBuild := Some(ScmInfo(url("https://github.com/PlayQ/d4s"), "scm:git:https://github.com/PlayQ/d4s.git")),
    scalacOptions in ThisBuild += """-Xmacro-settings:scalatest-version=VExpr(V.scalatest)""",
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      //publishArtifacts,
      setNextVersion,
      commitNextVersion,
      pushChanges
    ),
    onChangedBuildSource in Global := ReloadOnSourceChanges
  )
  .enablePlugins(IzumiPublishingPlugin, IzumiResolverPlugin)
  .aggregate(
    `d4s-agg`
  )
