addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.4.3")
addSbtPlugin(
  "com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % "0.18.21"
)
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.10.0")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

libraryDependencies ++= Seq(
  "com.lihaoyi" %% "sourcecode" % "0.4.2"
)

addSbtPlugin("org.scala-js" % "sbt-scalajs"       % "1.16.0")
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.10.0")

addDependencyTreePlugin
