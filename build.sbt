ThisBuild / scalaVersion := "3.4.2"
ThisBuild / organization := "beauty.scala"

def module(implicit name: sourcecode.Name) =
  Project(name.value, file("modules") / name.value)
    .settings(
      scalacOptions -= "-Xfatal-warnings",
      scalacOptions += "-no-indent",
    )

val shared = module
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value
    )
  )

val backend = module
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s"         % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      "org.http4s"                   %% "http4s-ember-server"     % "0.23.27",
    ),
    fork := true,
  )
  .dependsOn(shared)

val root = project
  .in(file("."))
  .aggregate(backend, shared)
