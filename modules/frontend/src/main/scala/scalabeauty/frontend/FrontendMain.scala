package scalabeauty.frontend

import cats.effect.IO
import cats.syntax.all.*
import org.http4s.client.Client
import org.http4s.dom.FetchClientBuilder
import org.http4s.implicits.*
import scalabeauty.api.*
import scalabeauty.api.Author.GithubCase
import smithy4s.http4s.SimpleRestJsonBuilder
import tyrian.*
import tyrian.Html.*

import scala.scalajs.js.annotation.JSExportTopLevel

enum Msg {
  case GotSnippets(data: List[Snippet])
  case ClickedSnippet(snippet: Snippet)
  case GoToUrl(url: String)
  case NoOp
}

case class Model(data: List[Snippet])

@JSExportTopLevel("TyrianApp")
object FrontendMain extends TyrianIOApp[Msg, Model] {
  given ScalaBeautyApi[IO] =
    SimpleRestJsonBuilder(ScalaBeautyApi)
      .client(resetBaseUri(FetchClientBuilder[IO].create))
      .uri(uri"/api")
      .make
      .toTry
      .get

  // https://github.com/disneystreaming/smithy4s/issues/1245
  private def resetBaseUri(c: Client[IO]): Client[IO] = Client[IO] { req =>
    val amendedUri     = req.uri.copy(scheme = None, authority = None)
    val amendedRequest = req.withUri(amendedUri)
    c.run(amendedRequest)
  }

  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = (
    Model(Nil),
    Cmd.Run(
      ScalaBeautyApi[IO]
        .getSnippets()
        .map { output =>
          Msg.GotSnippets(output.snippets)
        }
    ),
  )

  def view(model: Model): Html[Msg] =
    section(
      className := "section"
    )(
      div(
        className := "container"
      )(
        h1(
          className := "title"
        )("Scala.beauty"),
        p(
          className := "subtitle"
        )(
          text("Today's top snippets:")
        ),
        button(className := "button block")("Add yours"),
        if (model.data.isEmpty) text("Loading...")
        else
          ul(
            model.data.map { snippet =>
              div(
                className := "box",
                style("cursor", "pointer"),
                onClick(Msg.ClickedSnippet(snippet)),
              )(
                li(
                  div(
                    div(className := "block")(
                      span(className := "has-text-grey")("#" + snippet.id.value),
                      text(" by "),
                      snippet.author.match { case GithubCase(github) =>
                        val url = show"https://github.com/${github.username}"
                        a(href := url, onClick(Msg.GoToUrl(url)))(
                          show"@${github.username}"
                        )
                      },
                    ),
                    p(className := "block")(i(snippet.description)),
                    div(className := "block")(pre(snippet.code)),
                  )
                )
              )
            }
          ),
      )
    )

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = {
    case Msg.ClickedSnippet(snippet) =>
      (
        model,
        Cmd.SideEffect {
          println("Clicked snippet " + snippet)
        },
      )
    case Msg.GoToUrl(url) =>
      (
        model,
        // Workaround for Tyrian capturing clicks on anchors
        // even if they're new tabs
        Cmd.SideEffect {
          org.scalajs.dom.window.open(url, "_blank").focus()
        },
      )
    case Msg.GotSnippets(data) => (model.copy(data = data), Cmd.None)
    case Msg.NoOp              => (model, Cmd.None)
  }

  def subscriptions(model: Model): Sub[IO, Msg] = Sub.None

  def router: Location => Msg = Routing.none(Msg.NoOp)
}
