package scalabeauty.backend

import cats.syntax.all.*
import ciris.*
import com.comcast.ip4s.*

case class AppConfig(
    db: DbConfig,
    http: HttpConfig,
)

object AppConfig {
  val read: ConfigValue[Effect, AppConfig] =
    (
      DbConfig.read,
      HttpConfig.read,
    ).parMapN(AppConfig.apply)
}

case class DbConfig(
    host: String,
    port: Int,
    user: String,
    database: String,
    password: Secret[String],
)

object DbConfig {
  def fromUrl(url: String): DbConfig = url match {
    case s"postgres://$user:$password@$host:$port/$database" =>
      DbConfig(host, port.toInt, user, database, Secret(password))
  }

  val read: ConfigValue[Effect, DbConfig] = env("DATABASE_URL")
    .map(fromUrl)
    .default(
      DbConfig("localhost", 5432, "postgres", "postgres", Secret("postgres"))
    )
}

case class HttpConfig(
    port: Port
)

object HttpConfig {
  val read: ConfigValue[Effect, HttpConfig] = env("PORT")
    .as[Int]
    .map(Port.fromInt(_).get)
    .default(port"8080")
    .map(HttpConfig.apply)
}
