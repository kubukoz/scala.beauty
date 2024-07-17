import sbt.internal.ProjectMatrix
import sbt.VirtualAxis.ScalaVersionAxis

val scala3 = "3.4.2"

ThisBuild / organization := "beauty.scala"

val commonSettings = Seq(
  scalacOptions -= "-Xfatal-warnings",
  scalacOptions += "-no-indent",
  scalaVersion := scala3,
  libraryDependencies ++= Seq(
    compilerPlugin("org.polyvariant" % "better-tostring" % "0.3.17" cross CrossVersion.full),
    "com.disneystreaming" %%% "weaver-cats" % "0.8.4" % Test,
  ),
  publish / skip := true,
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

val yarnBuild = taskKey[File]("Build the web app. Returns the dist directory")

val frontend = module
  .enablePlugins(ScalaJSPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "io.indigoengine" %%% "tyrian-io"      % "0.10.0",
      "tech.neander"    %%% "smithy4s-fetch" % "0.0.4",
    ),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    yarnBuild := {
      import sys.process._
      Process(
        List(
          "yarn",
          "--cwd",
          baseDirectory.value.toString,
          "build",
        )
      ).!

      baseDirectory.value / "dist"
    },
  )
  .dependsOn(shared.js(scala3))

val isHeroku = settingKey[Boolean]("whether we're deploying to Heroku")

val backend = module
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s"                 % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger"         % smithy4sVersion.value,
      "org.http4s"                   %% "http4s-ember-server"             % "0.23.27",
      "org.tpolecat"                 %% "skunk-core"                      % "0.6.4",
      "is.cir"                       %% "ciris"                           % "3.6.0",
      "com.dimafeng"                 %% "testcontainers-scala-postgresql" % "0.41.4" % Test,
    ),
    fork               := true,
    dockerUpdateLatest := true,
    isHeroku           := false,
    dockerBuildOptions ++= { if (isHeroku.value) Seq("--platform", "linux/amd64") else Nil },
    dockerAlias := (
      if (isHeroku.value)
        DockerAlias(
          registryHost = Some("registry.heroku.com"),
          username = Some("scala-beauty"),
          name = "web",
          tag = None,
        )
      else
        DockerAlias(
          registryHost = None,
          username = None,
          name = "scala-beauty",
          tag = None,
        )
    ),
    dockerBaseImage := "openjdk:11-jre",

    // include frontend
    Compile / resourceGenerators += Def.task {
      import sys.process._

      val frontendDir = (frontend / yarnBuild).value
      val targetDir   = (Compile / resourceManaged).value / "frontend"

      IO.delete(targetDir)
      IO.copyDirectory(frontendDir, targetDir)

      Path.allSubpaths(targetDir).map(_._1).toList
    },
  )
  .dependsOn(shared.jvm(autoScalaLibrary = true))

val cli = module
  .enablePlugins(JavaAppPackaging)
  .settings(
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s"     % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-decline"    % smithy4sVersion.value,
      "org.http4s"                   %% "http4s-ember-client" % "0.23.27",
    ),
    fork := true,
  )
  .dependsOn(shared.jvm(autoScalaLibrary = true))

val root = project
  .in(file("."))
  .aggregate(backend, frontend)
  .aggregate(shared.projectRefs: _*)
  .settings(
    publish / skip := true,
    addCommandAlias(
      "herokuPush",
      List(
        "set backend/isHeroku := true",
        "backend/Docker/publish",
        "set backend/isHeroku := false",
      ).mkString(";"),
    ),
    TaskKey[String]("herokuRelease") := {
      import sys.process.*
      "heroku container:release web".!!
    },
    addCommandAlias("herokuDeploy", "herokuPush;herokuRelease"),
  )
