package scalabeauty.frontend

import cats.effect.IO

import scala.scalajs.js
import scala.scalajs.js.annotation.*

@js.native
@JSImport("shiki", JSImport.Namespace)
object ShikiFacade extends js.Object {
  def codeToHtml(code: String, options: js.Dynamic): js.Promise[String] = js.native
}

object Shiki {
  def codeToHtml(code: String, language: String, theme: String): IO[String] =
    IO.fromPromise {
      IO {
        ShikiFacade
          .codeToHtml(
            code,
            scalajs.js.Dynamic.literal(
              lang = language,
              theme = theme,
            ),
          )
      }
    }
}
