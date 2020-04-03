import IzumiPublishingPlugin.Keys._
import IzumiPublishingPlugin.autoImport._
import IzumiConvenienceTasksPlugin.Keys._
import sbtrelease.ReleaseStateTransformations._

enablePlugins(IzumiPublishingPlugin, IzumiResolverPlugin, IzumiConvenienceTasksPlugin, SbtgenVerificationPlugin)

lazy val `aws-common` = project.in(file("./aws-common"))
  .settings(
    libraryDependencies ++= Seq(
      compilerPlugin("org.typelevel" % "kind-projector" % V.kind_projector cross CrossVersion.full),
      "io.7mind.izumi" %% "distage-extension-plugins" % V.izumi_version,
      "io.7mind.izumi" %% "distage-extension-config" % V.izumi_version
    )
  )
  .settings(
    organization := "net.playq",
    unmanagedSourceDirectories in Compile += baseDirectory.value / ".jvm/src/main/scala" ,
    unmanagedResourceDirectories in Compile += baseDirectory.value / ".jvm/src/main/resources" ,
    unmanagedSourceDirectories in Test += baseDirectory.value / ".jvm/src/test/scala" ,
    unmanagedResourceDirectories in Test += baseDirectory.value / ".jvm/src/test/resources" ,
    unmanagedSourceDirectories in Compile := (unmanagedSourceDirectories in Compile).value.flatMap {
      dir =>
       Seq(dir, file(dir.getPath + (CrossVersion.partialVersion(scalaVersion.value) match {
         case Some((2, 11)) => "_2.11"
         case Some((2, 12)) => "_2.12"
         case Some((2, 13)) => "_2.13"
         case _             => "_3.0"
       })))
    },
    scalacOptions in Compile += "-Xmacro-settings:metricsRole=default",
    scalacOptions in Compile += s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsDir=${(classDirectory in Test).value}",
    scalacOptions in Compile += s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsRole=${(name in Test).value};${(moduleName in Test).value}",
    testOptions in Test += Tests.Argument("-oDF"),
    logBuffered in Test := true,
    resolvers += DefaultMavenRepository,
    scalaVersion := crossScalaVersions.value.head,
    crossScalaVersions := Seq(
      "2.13.1",
      "2.12.10"
    ),
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (_, "2.12.10") => Seq(
        "-Xsource:2.13",
        "-Ybackend-parallelism",
        math.min(16, math.max(1, sys.runtime.availableProcessors() - 1)).toString,
        "-Ypartial-unification",
        "-Yno-adapted-args",
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
      case (_, "2.13.1") => Seq(
        "-Xlint:_,-eta-sam",
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
      case (false, "2.12.10") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:izumi.**",
        "-opt-inline-from:net.playq.**"
      )
      case (false, "2.13.1") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:izumi.**",
        "-opt-inline-from:net.playq.**"
      )
      case (_, _) => Seq.empty
    } }
  )
  .enablePlugins(IzumiPublishingPlugin, IzumiResolverPlugin)

lazy val `metrics` = project.in(file("./metrics"))
  .settings(
    libraryDependencies ++= Seq(
      compilerPlugin("org.typelevel" % "kind-projector" % V.kind_projector cross CrossVersion.full),
      "io.7mind.izumi" %% "distage-framework" % V.izumi_version,
      "dev.zio" %% "zio" % V.zio,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
    )
  )
  .settings(
    organization := "net.playq",
    unmanagedSourceDirectories in Compile += baseDirectory.value / ".jvm/src/main/scala" ,
    unmanagedResourceDirectories in Compile += baseDirectory.value / ".jvm/src/main/resources" ,
    unmanagedSourceDirectories in Test += baseDirectory.value / ".jvm/src/test/scala" ,
    unmanagedResourceDirectories in Test += baseDirectory.value / ".jvm/src/test/resources" ,
    unmanagedSourceDirectories in Compile := (unmanagedSourceDirectories in Compile).value.flatMap {
      dir =>
       Seq(dir, file(dir.getPath + (CrossVersion.partialVersion(scalaVersion.value) match {
         case Some((2, 11)) => "_2.11"
         case Some((2, 12)) => "_2.12"
         case Some((2, 13)) => "_2.13"
         case _             => "_3.0"
       })))
    },
    scalacOptions in Compile += "-Xmacro-settings:metricsRole=default",
    scalacOptions in Compile += s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsDir=${(classDirectory in Test).value}",
    scalacOptions in Compile += s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsRole=${(name in Test).value};${(moduleName in Test).value}",
    testOptions in Test += Tests.Argument("-oDF"),
    logBuffered in Test := true,
    resolvers += DefaultMavenRepository,
    scalaVersion := crossScalaVersions.value.head,
    crossScalaVersions := Seq(
      "2.13.1",
      "2.12.10"
    ),
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (_, "2.12.10") => Seq(
        "-Xsource:2.13",
        "-Ybackend-parallelism",
        math.min(16, math.max(1, sys.runtime.availableProcessors() - 1)).toString,
        "-Ypartial-unification",
        "-Yno-adapted-args",
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
      case (_, "2.13.1") => Seq(
        "-Xlint:_,-eta-sam",
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
      case (false, "2.12.10") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:izumi.**",
        "-opt-inline-from:net.playq.**"
      )
      case (false, "2.13.1") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:izumi.**",
        "-opt-inline-from:net.playq.**"
      )
      case (_, _) => Seq.empty
    } }
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
      "dev.zio" %% "zio" % V.zio,
      "dev.zio" %% "zio-interop-cats" % V.zio_interop_cats,
      "co.fs2" %% "fs2-io" % V.fs2,
      "io.7mind.izumi" %% "fundamentals-bio" % V.izumi_version,
      "io.7mind.izumi" %% "distage-framework" % V.izumi_version,
      "io.7mind.izumi" %% "logstage-adapter-slf4j" % V.izumi_version,
      "software.amazon.awssdk" % "dynamodb" % V.aws_java_sdk_2 exclude("log4j", "log4j"),
      "software.amazon.awssdk" % "apache-client" % V.aws_java_sdk_2 exclude("log4j", "log4j"),
      "com.propensive" %% "magnolia" % V.magnolia_version,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
    )
  )
  .settings(
    organization := "net.playq",
    unmanagedSourceDirectories in Compile += baseDirectory.value / ".jvm/src/main/scala" ,
    unmanagedResourceDirectories in Compile += baseDirectory.value / ".jvm/src/main/resources" ,
    unmanagedSourceDirectories in Test += baseDirectory.value / ".jvm/src/test/scala" ,
    unmanagedResourceDirectories in Test += baseDirectory.value / ".jvm/src/test/resources" ,
    unmanagedSourceDirectories in Compile := (unmanagedSourceDirectories in Compile).value.flatMap {
      dir =>
       Seq(dir, file(dir.getPath + (CrossVersion.partialVersion(scalaVersion.value) match {
         case Some((2, 11)) => "_2.11"
         case Some((2, 12)) => "_2.12"
         case Some((2, 13)) => "_2.13"
         case _             => "_3.0"
       })))
    },
    scalacOptions in Compile += "-Xmacro-settings:metricsRole=default",
    scalacOptions in Compile += s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsDir=${(classDirectory in Test).value}",
    scalacOptions in Compile += s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsRole=${(name in Test).value};${(moduleName in Test).value}",
    testOptions in Test += Tests.Argument("-oDF"),
    logBuffered in Test := true,
    resolvers += DefaultMavenRepository,
    scalaVersion := crossScalaVersions.value.head,
    crossScalaVersions := Seq(
      "2.13.1",
      "2.12.10"
    ),
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (_, "2.12.10") => Seq(
        "-Xsource:2.13",
        "-Ybackend-parallelism",
        math.min(16, math.max(1, sys.runtime.availableProcessors() - 1)).toString,
        "-Ypartial-unification",
        "-Yno-adapted-args",
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
      case (_, "2.13.1") => Seq(
        "-Xlint:_,-eta-sam",
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
      case (false, "2.12.10") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:izumi.**",
        "-opt-inline-from:net.playq.**"
      )
      case (false, "2.13.1") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:izumi.**",
        "-opt-inline-from:net.playq.**"
      )
      case (_, _) => Seq.empty
    } }
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
      "org.scalatest" %% "scalatest" % V.scalatest % Test,
      "org.scalatestplus" %% "scalacheck-1-14" % V.scalatestplus_scalacheck % Test,
      "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % V.scalacheck_shapeless % Test
    )
  )
  .settings(
    organization := "net.playq",
    unmanagedSourceDirectories in Compile += baseDirectory.value / ".jvm/src/main/scala" ,
    unmanagedResourceDirectories in Compile += baseDirectory.value / ".jvm/src/main/resources" ,
    unmanagedSourceDirectories in Test += baseDirectory.value / ".jvm/src/test/scala" ,
    unmanagedResourceDirectories in Test += baseDirectory.value / ".jvm/src/test/resources" ,
    scalacOptions in Compile += "-Xmacro-settings:metricsRole=default",
    scalacOptions in Compile += s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsDir=${(classDirectory in Test).value}",
    scalacOptions in Compile += s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsRole=${(name in Test).value};${(moduleName in Test).value}",
    testOptions in Test += Tests.Argument("-oDF"),
    logBuffered in Test := true,
    resolvers += DefaultMavenRepository,
    scalaVersion := crossScalaVersions.value.head,
    crossScalaVersions := Seq(
      "2.13.1",
      "2.12.10"
    ),
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (_, "2.12.10") => Seq(
        "-Xsource:2.13",
        "-Ybackend-parallelism",
        math.min(16, math.max(1, sys.runtime.availableProcessors() - 1)).toString,
        "-Ypartial-unification",
        "-Yno-adapted-args",
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
      case (_, "2.13.1") => Seq(
        "-Xlint:_,-eta-sam",
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
      case (false, "2.12.10") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:izumi.**",
        "-opt-inline-from:net.playq.**"
      )
      case (false, "2.13.1") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:izumi.**",
        "-opt-inline-from:net.playq.**"
      )
      case (_, _) => Seq.empty
    } }
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
      "org.scalatestplus" %% "scalacheck-1-14" % V.scalatestplus_scalacheck % Test,
      "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % V.scalacheck_shapeless % Test
    )
  )
  .settings(
    organization := "net.playq",
    unmanagedSourceDirectories in Compile += baseDirectory.value / ".jvm/src/main/scala" ,
    unmanagedResourceDirectories in Compile += baseDirectory.value / ".jvm/src/main/resources" ,
    unmanagedSourceDirectories in Test += baseDirectory.value / ".jvm/src/test/scala" ,
    unmanagedResourceDirectories in Test += baseDirectory.value / ".jvm/src/test/resources" ,
    scalacOptions in Compile += "-Xmacro-settings:metricsRole=default",
    scalacOptions in Compile += s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsDir=${(classDirectory in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsDir=${(classDirectory in Test).value}",
    scalacOptions in Compile += s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsRole=${(name in Compile).value};${(moduleName in Compile).value}",
    scalacOptions in Test += s"-Xmacro-settings:metricsRole=${(name in Test).value};${(moduleName in Test).value}",
    testOptions in Test += Tests.Argument("-oDF"),
    logBuffered in Test := true,
    resolvers += DefaultMavenRepository,
    scalaVersion := crossScalaVersions.value.head,
    crossScalaVersions := Seq(
      "2.13.1",
      "2.12.10"
    ),
    scalacOptions ++= { (isSnapshot.value, scalaVersion.value) match {
      case (_, "2.12.10") => Seq(
        "-Xsource:2.13",
        "-Ybackend-parallelism",
        math.min(16, math.max(1, sys.runtime.availableProcessors() - 1)).toString,
        "-Ypartial-unification",
        "-Yno-adapted-args",
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
      case (_, "2.13.1") => Seq(
        "-Xlint:_,-eta-sam",
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
      case (false, "2.12.10") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:izumi.**",
        "-opt-inline-from:net.playq.**"
      )
      case (false, "2.13.1") => Seq(
        "-opt:l:inline",
        "-opt-inline-from:izumi.**",
        "-opt-inline-from:net.playq.**"
      )
      case (_, _) => Seq.empty
    } }
  )
  .enablePlugins(IzumiPublishingPlugin, IzumiResolverPlugin)

lazy val `d4s-agg` = (project in file(".agg/.-d4s-agg"))
  .settings(
    skip in publish := true
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
    skip in publish := true
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
    skip in publish := true
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
      s"-Xmacro-settings:product-version=${version.value}",
      s"-Xmacro-settings:product-group=${organization.value}",
      s"-Xmacro-settings:sbt-version=${sbtVersion.value}"
    ),
    fork in Global := false,
    crossScalaVersions := Nil,
    scalaVersion := "2.13.1",
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
