import sbt.internal.ProjectMatrix
import sbt.VirtualAxis.ScalaVersionAxis

val scala3 = "3.4.2"

ThisBuild / organization := "beauty.scala"

val commonSettings = Seq(
  scalacOptions -= "-Xfatal-warnings",
  scalacOptions += "-no-indent",
  scalaVersion := scala3,
  libraryDependencies ++= Seq(
    "com.disneystreaming" %%% "weaver-cats" % "0.8.4" % Test
  ),
)

def module(implicit name: sourcecode.Name) =
  Project(name.value, file("modules") / name.value)
    .settings(commonSettings)

def crossModule(implicit name: sourcecode.Name) =
  ProjectMatrix(name.value, file("modules") / name.value)
    .jvmPlatform(Seq(scala3))
    .jsPlatform(Seq(scala3))
    .defaultAxes(VirtualAxis.scalaVersionAxis(scala3, scala3))
    .settings(commonSettings)

val shared =
  crossModule
    .enablePlugins(Smithy4sCodegenPlugin)
    .settings(
      libraryDependencies ++= Seq(
        "com.disneystreaming.smithy4s" %%% "smithy4s-core" % smithy4sVersion.value
      )
    )

val frontend = module
  .enablePlugins(ScalaJSPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "io.indigoengine" %%% "tyrian-io"      % "0.10.0",
      "tech.neander"    %%% "smithy4s-fetch" % "0.0.4",
    ),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
  )
  .dependsOn(shared.js(scala3))

val typoModels = module
  .settings(
    libraryDependencies ++= Seq(
      "com.olvind.typo" %% "typo-dsl-doobie" % "0.22.2"
    )
  )

val backend = module
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s"                 % smithy4s.codegen.BuildInfo.version,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger"         % smithy4s.codegen.BuildInfo.version,
      "org.http4s"                   %% "http4s-ember-server"             % "0.23.27",
      "org.tpolecat"                 %% "skunk-core"                      % "0.6.4",
      "com.dimafeng"                 %% "testcontainers-scala-postgresql" % "0.41.4" % Test,
      "com.olvind.typo"              %% "typo-dsl-doobie"                 % "0.22.2",
    ),
    fork := true,
  )
  .dependsOn(shared.jvm(autoScalaLibrary = true), typoModels)

val typo = module
  .settings(
    libraryDependencies ++= Seq(
      "com.olvind.typo" %% "typo" % "0.22.2"
    ),
    fork := true,
  )

val root = project
  .in(file("."))
  .aggregate(backend, frontend)
  .aggregate(shared.projectRefs: _*)
