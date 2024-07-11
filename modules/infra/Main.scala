import besom.*
import besom.api.heroku.*
import besom.api.heroku.addon.Addon
import besom.api.heroku.addon.AddonArgs
import besom.api.heroku.app.App
import besom.api.heroku.app.AppArgs
import besom.api.heroku.build.inputs.BuildSourceArgs
import besom.api.heroku.build.Build
import besom.api.heroku.build.BuildArgs

@main def main = Pulumi.run {

  val app = App(
    name = "scala-beauty",
    args = AppArgs(
      region = "eu",
      stack = "container",
    ),
  )

  val postgresAddon = Addon(
    name = "postgres",
    args = AddonArgs(
      appId = app.id,
      plan = "heroku-postgresql:essential-0",
    ),
  )

  val build = Build(
    "build",
    BuildArgs(
      appId = app.id,
      source = BuildSourceArgs(
        // separate directory so that its checksum doesn't get affected when we change the build script
        path = "build"
      ),
    ),
  )

  Stack(postgresAddon, build).exports(
    appName = app.name,
    url = app.webUrl,
    postgresVars = postgresAddon.configVarValues,
  )
}
