package scalabeauty.frontend

import cats.effect.IO
import cats.syntax.all.*
import org.http4s.client.Client
import org.http4s.dom.FetchClientBuilder
import org.http4s.implicits.*
import scalabeauty.api.ScalaBeautyApi
import scalabeauty.api.Snippet
import smithy4s.http4s.SimpleRestJsonBuilder
import tyrian.*
import tyrian.Html.*

import scala.scalajs.js.annotation.JSExportTopLevel

enum Msg {
  case GetSnippets()
  case GotSnippets(data: List[Snippet])
  case NoOp
}

case class Model(data: List[Snippet])

@JSExportTopLevel("TyrianApp")
object FrontendMain extends TyrianIOApp[Msg, Model] {
  private val client =
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

  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = (Model(Nil), Cmd.emit(Msg.GetSnippets()))

  def view(model: Model): Html[Msg] = div(
    text("Hello world! Today's top snippets:"),
    ul(
      model.data.map { snippet =>
        li(
          div(
            text(s"Author: ${snippet.author}"),
            div(s"Description: ${snippet.description}"),
            pre(snippet.code),
          )
        )
      }
    ),
  )

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = {
    case Msg.GetSnippets() =>
      (
        model,
        Cmd.Run(
          client.getSnippets().map { output =>
            Msg.GotSnippets(output.snippets)
          }
        ),
      )
    case Msg.GotSnippets(data) => (model.copy(data = data), Cmd.None)
    case Msg.NoOp              => (model, Cmd.None)
  }

  def subscriptions(model: Model): Sub[IO, Msg] = Sub.None

  def router: Location => Msg = Routing.none(Msg.NoOp)
}
