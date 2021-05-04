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
    crossScalaVersions := Seq(
      "2.13.5",
      "2.12.13"
    ),
    scalaVersion := crossScalaVersions.value.head,
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (_, "2.12.13") => Seq(
        "-Xsource:2.13",
        "-Ypartial-unification",
        if (insideCI.value) "-Wconf:any:error" else "-Wconf:any:warning",
        "-Wconf:cat=optimizer:warning",
        "-Wconf:cat=other-match-analysis:error",
        "-Wconf:msg=kind-projector:silent",
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
      case (_, "2.13.5") => Seq(
        "-Xlint:_,-eta-sam,-multiarg-infix,-byname-implicit",
        if (insideCI.value) "-Wconf:any:error" else "-Wconf:any:warning",
        "-Wconf:cat=optimizer:warning",
        "-Wconf:cat=other-match-analysis:error",
        "-Wconf:msg=kind-projector:silent",
        "-Ybackend-parallelism",
        math.min(16, math.max(1, sys.runtime.availableProcessors() - 1)).toString,
        "-Wdead-code",
        "-Wextra-implicit",
        "-Wnumeric-widen",
        "-Woctal-literal",
        "-Wvalue-discard",
        "-Wunused:_",
        "-Wmacros:after",
        "-Ycache-plugin-class-loader:always",
        "-Ycache-macro-class-loader:last-modified"
      )
      case (_, _) => Seq.empty
    } },
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (false, "2.12.13") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:net.playq.**"
      )
      case (false, "2.13.5") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:net.playq.**"
      )
      case (_, _) => Seq.empty
    } },
    organization := "net.playq",
    Compile / unmanagedSourceDirectories += baseDirectory.value / ".jvm/src/main/scala" ,
    Compile / unmanagedResourceDirectories += baseDirectory.value / ".jvm/src/main/resources" ,
    Test / unmanagedSourceDirectories += baseDirectory.value / ".jvm/src/test/scala" ,
    Test / unmanagedResourceDirectories += baseDirectory.value / ".jvm/src/test/resources" ,
    Compile / scalacOptions += s"-Xmacro-settings:metricsDir=${(Compile / classDirectory).value}",
    Test / scalacOptions += s"-Xmacro-settings:metricsDir=${(Compile / classDirectory).value}",
    Test / scalacOptions += s"-Xmacro-settings:metricsDir=${(Test / classDirectory).value}",
    Compile / scalacOptions += s"-Xmacro-settings:metricsRole=${(Compile / name).value};${(Compile / moduleName).value}",
    Test / scalacOptions += s"-Xmacro-settings:metricsRole=${(Compile / name).value};${(Compile / moduleName).value}",
    Test / scalacOptions += s"-Xmacro-settings:metricsRole=${(Test / name).value};${(Test / moduleName).value}",
    Test / testOptions += Tests.Argument("-oDF"),
    Test / logBuffered := true,
    resolvers += DefaultMavenRepository,
    resolvers += Opts.resolver.sonatypeSnapshots,
    Compile / unmanagedSourceDirectories ++= (Compile / unmanagedSourceDirectories).value.flatMap {
      dir =>
       val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
       def scalaDir(s: String) = file(dir.getPath + s)
       (partialVersion match {
         case Some((2, n)) => Seq(scalaDir("_2"), scalaDir("_2." + n.toString))
         case Some((x, n)) => Seq(scalaDir("_3"), scalaDir("_" + x.toString + "." + n.toString))
         case None         => Seq.empty
       })
    },
    Test / unmanagedSourceDirectories ++= (Test / unmanagedSourceDirectories).value.flatMap {
      dir =>
       val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
       def scalaDir(s: String) = file(dir.getPath + s)
       (partialVersion match {
         case Some((2, n)) => Seq(scalaDir("_2"), scalaDir("_2." + n.toString))
         case Some((x, n)) => Seq(scalaDir("_3"), scalaDir("_" + x.toString + "." + n.toString))
         case None         => Seq.empty
       })
    },
    Compile / scalacOptions += "-Xmacro-settings:metricsRole=default"
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
    crossScalaVersions := Seq(
      "2.13.5",
      "2.12.13"
    ),
    scalaVersion := crossScalaVersions.value.head,
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (_, "2.12.13") => Seq(
        "-Xsource:2.13",
        "-Ypartial-unification",
        if (insideCI.value) "-Wconf:any:error" else "-Wconf:any:warning",
        "-Wconf:cat=optimizer:warning",
        "-Wconf:cat=other-match-analysis:error",
        "-Wconf:msg=kind-projector:silent",
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
      case (_, "2.13.5") => Seq(
        "-Xlint:_,-eta-sam,-multiarg-infix,-byname-implicit",
        if (insideCI.value) "-Wconf:any:error" else "-Wconf:any:warning",
        "-Wconf:cat=optimizer:warning",
        "-Wconf:cat=other-match-analysis:error",
        "-Wconf:msg=kind-projector:silent",
        "-Ybackend-parallelism",
        math.min(16, math.max(1, sys.runtime.availableProcessors() - 1)).toString,
        "-Wdead-code",
        "-Wextra-implicit",
        "-Wnumeric-widen",
        "-Woctal-literal",
        "-Wvalue-discard",
        "-Wunused:_",
        "-Wmacros:after",
        "-Ycache-plugin-class-loader:always",
        "-Ycache-macro-class-loader:last-modified"
      )
      case (_, _) => Seq.empty
    } },
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (false, "2.12.13") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:net.playq.**"
      )
      case (false, "2.13.5") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:net.playq.**"
      )
      case (_, _) => Seq.empty
    } },
    organization := "net.playq",
    Compile / unmanagedSourceDirectories += baseDirectory.value / ".jvm/src/main/scala" ,
    Compile / unmanagedResourceDirectories += baseDirectory.value / ".jvm/src/main/resources" ,
    Test / unmanagedSourceDirectories += baseDirectory.value / ".jvm/src/test/scala" ,
    Test / unmanagedResourceDirectories += baseDirectory.value / ".jvm/src/test/resources" ,
    Compile / scalacOptions += s"-Xmacro-settings:metricsDir=${(Compile / classDirectory).value}",
    Test / scalacOptions += s"-Xmacro-settings:metricsDir=${(Compile / classDirectory).value}",
    Test / scalacOptions += s"-Xmacro-settings:metricsDir=${(Test / classDirectory).value}",
    Compile / scalacOptions += s"-Xmacro-settings:metricsRole=${(Compile / name).value};${(Compile / moduleName).value}",
    Test / scalacOptions += s"-Xmacro-settings:metricsRole=${(Compile / name).value};${(Compile / moduleName).value}",
    Test / scalacOptions += s"-Xmacro-settings:metricsRole=${(Test / name).value};${(Test / moduleName).value}",
    Test / testOptions += Tests.Argument("-oDF"),
    Test / logBuffered := true,
    resolvers += DefaultMavenRepository,
    resolvers += Opts.resolver.sonatypeSnapshots,
    Compile / unmanagedSourceDirectories ++= (Compile / unmanagedSourceDirectories).value.flatMap {
      dir =>
       val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
       def scalaDir(s: String) = file(dir.getPath + s)
       (partialVersion match {
         case Some((2, n)) => Seq(scalaDir("_2"), scalaDir("_2." + n.toString))
         case Some((x, n)) => Seq(scalaDir("_3"), scalaDir("_" + x.toString + "." + n.toString))
         case None         => Seq.empty
       })
    },
    Test / unmanagedSourceDirectories ++= (Test / unmanagedSourceDirectories).value.flatMap {
      dir =>
       val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
       def scalaDir(s: String) = file(dir.getPath + s)
       (partialVersion match {
         case Some((2, n)) => Seq(scalaDir("_2"), scalaDir("_2." + n.toString))
         case Some((x, n)) => Seq(scalaDir("_3"), scalaDir("_" + x.toString + "." + n.toString))
         case None         => Seq.empty
       })
    },
    Compile / scalacOptions += "-Xmacro-settings:metricsRole=default"
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
    crossScalaVersions := Seq(
      "2.13.5",
      "2.12.13"
    ),
    scalaVersion := crossScalaVersions.value.head,
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (_, "2.12.13") => Seq(
        "-Xsource:2.13",
        "-Ypartial-unification",
        if (insideCI.value) "-Wconf:any:error" else "-Wconf:any:warning",
        "-Wconf:cat=optimizer:warning",
        "-Wconf:cat=other-match-analysis:error",
        "-Wconf:msg=kind-projector:silent",
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
      case (_, "2.13.5") => Seq(
        "-Xlint:_,-eta-sam,-multiarg-infix,-byname-implicit",
        if (insideCI.value) "-Wconf:any:error" else "-Wconf:any:warning",
        "-Wconf:cat=optimizer:warning",
        "-Wconf:cat=other-match-analysis:error",
        "-Wconf:msg=kind-projector:silent",
        "-Ybackend-parallelism",
        math.min(16, math.max(1, sys.runtime.availableProcessors() - 1)).toString,
        "-Wdead-code",
        "-Wextra-implicit",
        "-Wnumeric-widen",
        "-Woctal-literal",
        "-Wvalue-discard",
        "-Wunused:_",
        "-Wmacros:after",
        "-Ycache-plugin-class-loader:always",
        "-Ycache-macro-class-loader:last-modified"
      )
      case (_, _) => Seq.empty
    } },
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (false, "2.12.13") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:net.playq.**"
      )
      case (false, "2.13.5") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:net.playq.**"
      )
      case (_, _) => Seq.empty
    } },
    organization := "net.playq",
    Compile / unmanagedSourceDirectories += baseDirectory.value / ".jvm/src/main/scala" ,
    Compile / unmanagedResourceDirectories += baseDirectory.value / ".jvm/src/main/resources" ,
    Test / unmanagedSourceDirectories += baseDirectory.value / ".jvm/src/test/scala" ,
    Test / unmanagedResourceDirectories += baseDirectory.value / ".jvm/src/test/resources" ,
    Compile / scalacOptions += s"-Xmacro-settings:metricsDir=${(Compile / classDirectory).value}",
    Test / scalacOptions += s"-Xmacro-settings:metricsDir=${(Compile / classDirectory).value}",
    Test / scalacOptions += s"-Xmacro-settings:metricsDir=${(Test / classDirectory).value}",
    Compile / scalacOptions += s"-Xmacro-settings:metricsRole=${(Compile / name).value};${(Compile / moduleName).value}",
    Test / scalacOptions += s"-Xmacro-settings:metricsRole=${(Compile / name).value};${(Compile / moduleName).value}",
    Test / scalacOptions += s"-Xmacro-settings:metricsRole=${(Test / name).value};${(Test / moduleName).value}",
    Test / testOptions += Tests.Argument("-oDF"),
    Test / logBuffered := true,
    resolvers += DefaultMavenRepository,
    resolvers += Opts.resolver.sonatypeSnapshots,
    Compile / unmanagedSourceDirectories ++= (Compile / unmanagedSourceDirectories).value.flatMap {
      dir =>
       val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
       def scalaDir(s: String) = file(dir.getPath + s)
       (partialVersion match {
         case Some((2, n)) => Seq(scalaDir("_2"), scalaDir("_2." + n.toString))
         case Some((x, n)) => Seq(scalaDir("_3"), scalaDir("_" + x.toString + "." + n.toString))
         case None         => Seq.empty
       })
    },
    Test / unmanagedSourceDirectories ++= (Test / unmanagedSourceDirectories).value.flatMap {
      dir =>
       val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
       def scalaDir(s: String) = file(dir.getPath + s)
       (partialVersion match {
         case Some((2, n)) => Seq(scalaDir("_2"), scalaDir("_2." + n.toString))
         case Some((x, n)) => Seq(scalaDir("_3"), scalaDir("_" + x.toString + "." + n.toString))
         case None         => Seq.empty
       })
    },
    Compile / scalacOptions += "-Xmacro-settings:metricsRole=default"
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
    crossScalaVersions := Seq(
      "2.13.5",
      "2.12.13"
    ),
    scalaVersion := crossScalaVersions.value.head,
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (_, "2.12.13") => Seq(
        "-Xsource:2.13",
        "-Ypartial-unification",
        if (insideCI.value) "-Wconf:any:error" else "-Wconf:any:warning",
        "-Wconf:cat=optimizer:warning",
        "-Wconf:cat=other-match-analysis:error",
        "-Wconf:msg=kind-projector:silent",
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
      case (_, "2.13.5") => Seq(
        "-Xlint:_,-eta-sam,-multiarg-infix,-byname-implicit",
        if (insideCI.value) "-Wconf:any:error" else "-Wconf:any:warning",
        "-Wconf:cat=optimizer:warning",
        "-Wconf:cat=other-match-analysis:error",
        "-Wconf:msg=kind-projector:silent",
        "-Ybackend-parallelism",
        math.min(16, math.max(1, sys.runtime.availableProcessors() - 1)).toString,
        "-Wdead-code",
        "-Wextra-implicit",
        "-Wnumeric-widen",
        "-Woctal-literal",
        "-Wvalue-discard",
        "-Wunused:_",
        "-Wmacros:after",
        "-Ycache-plugin-class-loader:always",
        "-Ycache-macro-class-loader:last-modified"
      )
      case (_, _) => Seq.empty
    } },
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (false, "2.12.13") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:net.playq.**"
      )
      case (false, "2.13.5") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:net.playq.**"
      )
      case (_, _) => Seq.empty
    } },
    organization := "net.playq",
    Compile / unmanagedSourceDirectories += baseDirectory.value / ".jvm/src/main/scala" ,
    Compile / unmanagedResourceDirectories += baseDirectory.value / ".jvm/src/main/resources" ,
    Test / unmanagedSourceDirectories += baseDirectory.value / ".jvm/src/test/scala" ,
    Test / unmanagedResourceDirectories += baseDirectory.value / ".jvm/src/test/resources" ,
    Compile / scalacOptions += s"-Xmacro-settings:metricsDir=${(Compile / classDirectory).value}",
    Test / scalacOptions += s"-Xmacro-settings:metricsDir=${(Compile / classDirectory).value}",
    Test / scalacOptions += s"-Xmacro-settings:metricsDir=${(Test / classDirectory).value}",
    Compile / scalacOptions += s"-Xmacro-settings:metricsRole=${(Compile / name).value};${(Compile / moduleName).value}",
    Test / scalacOptions += s"-Xmacro-settings:metricsRole=${(Compile / name).value};${(Compile / moduleName).value}",
    Test / scalacOptions += s"-Xmacro-settings:metricsRole=${(Test / name).value};${(Test / moduleName).value}",
    Test / testOptions += Tests.Argument("-oDF"),
    Test / logBuffered := true,
    resolvers += DefaultMavenRepository,
    resolvers += Opts.resolver.sonatypeSnapshots,
    Compile / unmanagedSourceDirectories ++= (Compile / unmanagedSourceDirectories).value.flatMap {
      dir =>
       val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
       def scalaDir(s: String) = file(dir.getPath + s)
       (partialVersion match {
         case Some((2, n)) => Seq(scalaDir("_2"), scalaDir("_2." + n.toString))
         case Some((x, n)) => Seq(scalaDir("_3"), scalaDir("_" + x.toString + "." + n.toString))
         case None         => Seq.empty
       })
    },
    Test / unmanagedSourceDirectories ++= (Test / unmanagedSourceDirectories).value.flatMap {
      dir =>
       val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
       def scalaDir(s: String) = file(dir.getPath + s)
       (partialVersion match {
         case Some((2, n)) => Seq(scalaDir("_2"), scalaDir("_2." + n.toString))
         case Some((x, n)) => Seq(scalaDir("_3"), scalaDir("_" + x.toString + "." + n.toString))
         case None         => Seq.empty
       })
    },
    Compile / scalacOptions += "-Xmacro-settings:metricsRole=default"
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
    crossScalaVersions := Seq(
      "2.13.5",
      "2.12.13"
    ),
    scalaVersion := crossScalaVersions.value.head,
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (_, "2.12.13") => Seq(
        "-Xsource:2.13",
        "-Ypartial-unification",
        if (insideCI.value) "-Wconf:any:error" else "-Wconf:any:warning",
        "-Wconf:cat=optimizer:warning",
        "-Wconf:cat=other-match-analysis:error",
        "-Wconf:msg=kind-projector:silent",
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
      case (_, "2.13.5") => Seq(
        "-Xlint:_,-eta-sam,-multiarg-infix,-byname-implicit",
        if (insideCI.value) "-Wconf:any:error" else "-Wconf:any:warning",
        "-Wconf:cat=optimizer:warning",
        "-Wconf:cat=other-match-analysis:error",
        "-Wconf:msg=kind-projector:silent",
        "-Ybackend-parallelism",
        math.min(16, math.max(1, sys.runtime.availableProcessors() - 1)).toString,
        "-Wdead-code",
        "-Wextra-implicit",
        "-Wnumeric-widen",
        "-Woctal-literal",
        "-Wvalue-discard",
        "-Wunused:_",
        "-Wmacros:after",
        "-Ycache-plugin-class-loader:always",
        "-Ycache-macro-class-loader:last-modified"
      )
      case (_, _) => Seq.empty
    } },
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (false, "2.12.13") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:net.playq.**"
      )
      case (false, "2.13.5") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:net.playq.**"
      )
      case (_, _) => Seq.empty
    } },
    organization := "net.playq",
    Compile / unmanagedSourceDirectories += baseDirectory.value / ".jvm/src/main/scala" ,
    Compile / unmanagedResourceDirectories += baseDirectory.value / ".jvm/src/main/resources" ,
    Test / unmanagedSourceDirectories += baseDirectory.value / ".jvm/src/test/scala" ,
    Test / unmanagedResourceDirectories += baseDirectory.value / ".jvm/src/test/resources" ,
    Compile / scalacOptions += s"-Xmacro-settings:metricsDir=${(Compile / classDirectory).value}",
    Test / scalacOptions += s"-Xmacro-settings:metricsDir=${(Compile / classDirectory).value}",
    Test / scalacOptions += s"-Xmacro-settings:metricsDir=${(Test / classDirectory).value}",
    Compile / scalacOptions += s"-Xmacro-settings:metricsRole=${(Compile / name).value};${(Compile / moduleName).value}",
    Test / scalacOptions += s"-Xmacro-settings:metricsRole=${(Compile / name).value};${(Compile / moduleName).value}",
    Test / scalacOptions += s"-Xmacro-settings:metricsRole=${(Test / name).value};${(Test / moduleName).value}",
    Test / testOptions += Tests.Argument("-oDF"),
    Test / logBuffered := true,
    resolvers += DefaultMavenRepository,
    resolvers += Opts.resolver.sonatypeSnapshots,
    Compile / unmanagedSourceDirectories ++= (Compile / unmanagedSourceDirectories).value.flatMap {
      dir =>
       val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
       def scalaDir(s: String) = file(dir.getPath + s)
       (partialVersion match {
         case Some((2, n)) => Seq(scalaDir("_2"), scalaDir("_2." + n.toString))
         case Some((x, n)) => Seq(scalaDir("_3"), scalaDir("_" + x.toString + "." + n.toString))
         case None         => Seq.empty
       })
    },
    Test / unmanagedSourceDirectories ++= (Test / unmanagedSourceDirectories).value.flatMap {
      dir =>
       val partialVersion = CrossVersion.partialVersion(scalaVersion.value)
       def scalaDir(s: String) = file(dir.getPath + s)
       (partialVersion match {
         case Some((2, n)) => Seq(scalaDir("_2"), scalaDir("_2." + n.toString))
         case Some((x, n)) => Seq(scalaDir("_3"), scalaDir("_" + x.toString + "." + n.toString))
         case None         => Seq.empty
       })
    },
    Compile / scalacOptions += "-Xmacro-settings:metricsRole=default"
  )
  .enablePlugins(IzumiPublishingPlugin, IzumiResolverPlugin)

lazy val `d4s-agg` = (project in file(".agg/.-d4s-agg"))
  .settings(
    publish / skip := true,
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
    publish / skip := true,
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
    publish / skip := true,
    crossScalaVersions := Nil
  )
  .aggregate(
    `d4s-agg-jvm`
  )

lazy val `d4s-root` = (project in file("."))
  .settings(
    publish / skip := true,
    ThisBuild / publishMavenStyle := true,
    ThisBuild / scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-target:jvm-1.8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-language:higherKinds",
      "-explaintypes"
    ),
    ThisBuild / javacOptions ++= Seq(
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
    ThisBuild / scalacOptions ++= Seq(
      s"-Xmacro-settings:sbt-version=${sbtVersion.value}",
      s"-Xmacro-settings:git-repo-clean=${com.typesafe.sbt.SbtGit.GitKeys.gitUncommittedChanges.value}",
      s"-Xmacro-settings:git-branch=${com.typesafe.sbt.SbtGit.GitKeys.gitCurrentBranch.value}",
      s"-Xmacro-settings:git-described-version=${com.typesafe.sbt.SbtGit.GitKeys.gitDescribedVersion.value.getOrElse("")}",
      s"-Xmacro-settings:git-head-commit=${com.typesafe.sbt.SbtGit.GitKeys.gitHeadCommit.value.getOrElse("")}"
    ),
    Global / fork := false,
    crossScalaVersions := Nil,
    scalaVersion := "2.13.5",
    Global / coverageOutputXML := true,
    Global / coverageOutputHTML := true,
    Global / organization := "net.playq",
    sonatypeProfileName := "net.playq",
    sonatypeSessionName := s"[sbt-sonatype] ${name.value} ${version.value} ${java.util.UUID.randomUUID}",
    ThisBuild / publishTo := 
    (if (!isSnapshot.value) {
        sonatypePublishToBundle.value
      } else {
        Some(Opts.resolver.sonatypeSnapshots)
    })
    ,
    ThisBuild / credentials += Credentials(file(".secrets/credentials.sonatype-nexus.properties")),
    ThisBuild / homepage := Some(url("https://www.playq.com/")),
    ThisBuild / licenses := Seq("Apache-License" -> url("https://opensource.org/licenses/Apache-2.0")),
    ThisBuild / developers := List(
              Developer(id = "playq", name = "PlayQ", url = url("https://github.com/PlayQ"), email = "platform-team@playq.net"),
            ),
    ThisBuild / scmInfo := Some(ScmInfo(url("https://github.com/PlayQ/d4s"), "scm:git:https://github.com/PlayQ/d4s.git")),
    ThisBuild / scalacOptions += """-Xmacro-settings:scalatest-version=VExpr(V.scalatest)""",
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
    Global / onChangedBuildSource := ReloadOnSourceChanges
  )
  .enablePlugins(IzumiPublishingPlugin, IzumiResolverPlugin)
  .aggregate(
    `d4s-agg`
  )
