package scalabeauty.frontend

import tyrian.*
import tyrian.Html.*

import HtmlUtils.*

object Footer {

  val view: Html[Msg] =
    footer(
      className := "footer is-flex-align-items-flex-end mt-auto"
    )(
      div(className := "content has-text-centered")(
        text("Built with "),
        a(linkAttrs("https://scala-lang.org"))("Scala"),
        text(", "),
        a(linkAttrs("https://www.scala-js.org"))("Scala.js"),
        text(", "),
        a(linkAttrs("https://tyrian.indigoengine.io"))("Tyrian"),
        text(", "),
        a(linkAttrs("https://disneystreaming.github.io/smithy4s"))("Smithy4s"),
        text(", "),
        a(linkAttrs("https://bulma.io"))("Bulma"),
        text(" and "),
        a(linkAttrs("https://http4s.org"))("http4s"),
        text("."),
      )
    )

}
