package scalabeauty.frontend

import cats.effect.IO
import cats.syntax.all.*
import org.http4s.client.Client
import org.http4s.dom.FetchClientBuilder
import org.http4s.implicits.*
import scalabeauty.api
import scalabeauty.api.*
import scalabeauty.api.Author.GithubCase
import smithy4s.http4s.SimpleRestJsonBuilder
import tyrian.*
import tyrian.Html.*

import scala.scalajs.js.annotation.JSExportTopLevel

enum Msg {
  case GotSnippets(data: List[Snippet])
  case NavigateTo(url: String)
  case NewTab(url: String)
  case OpenSnippet(id: Slug)
  case GoHome
  case GoHomeResetState
  case OpenedSnippet(snippet: Snippet)
  case NoOp
  case SetTitle(title: String)
}

case class Model(page: Page) {
  def mapPage(f: Page => Page): Model = copy(page = f(page))
}

enum Page {
  case Home(data: List[api.Snippet])
  case Snippet(snippet: Option[api.Snippet])

  def mapHome(f: Page.Home => Page.Home): Page = this match {
    case h: Home    => f(h)
    case s: Snippet => s
  }
}

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

  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = initialize

  private def initialize: (Model, Cmd[IO, Msg]) =
    (
      Model(Page.Home(Nil)),
      Cmd.emit(Msg.SetTitle("Scala.beauty"))
        |+| Cmd
          .Run(ScalaBeautyApi[IO].getSnippets(None, None))
          .map(out => Msg.GotSnippets(out.snippets)),
    )

  def view(model: Model): Html[Msg] =
    model.page.match {
      case h: Page.Home             => viewHome(h)
      case Page.Snippet(None)       => viewSnippetPlaceholder
      case Page.Snippet(Some(snip)) => viewSnippet(snip)
    }

  private def viewGeneric(content: Elem[Msg]*) =
    section(
      className := "section"
    )(
      div(
        className := "container"
      )(content.toList)
    )

  private def viewHome(page: Page.Home) =
    viewGeneric(
      header(text("Scala.beauty")),
      subtitle(
        text("Today's top snippets:")
      ),
      button(className := "button block")("Add yours"),
      if (page.data.isEmpty) div(className := "block")(text("Loading..."))
      else
        ul(
          page.data.map { snippet =>
            li(className := "block")(
              a(href := "/snippet/" + snippet.id)(
                div(className := "box")(
                  div(className := "block")(
                    viewSlug(snippet.id),
                    text(" by "),
                    viewAuthor(snippet.author),
                  ),
                  p(className := "block")(i(snippet.description)),
                  div(className := "block")(pre(code(snippet.code))),
                )
              )
            )

          }
        ),
    )

  def viewSlug(slug: Slug)               = span(className := "has-text-grey is-family-monospace")(slug.hashed)
  extension (s: Slug) def hashed: String = "#" + s.value

  private def header(items: Elem[Msg]*) =
    h1(
      className := "title"
    )(items.toList)

  private def subtitle(items: Elem[Msg]*) =
    p(
      className := "subtitle"
    )(items.toList)

  private def viewAuthor(author: Author) = author.match { case GithubCase(github) =>
    val url = show"https://github.com/${github.username}"
    a(href := url, onClick(Msg.NewTab(url)))(
      show"@${github.username}"
    )
  }

  private def viewSnippet(snippet: Snippet) = viewGeneric(
    header(
      text("Scala."),
      viewSlug(snippet.id),
      text(".beauty"),
    ),
    subtitle(
      text(" by "),
      viewAuthor(snippet.author),
    ),
    p(className := "block")(i(snippet.description)),
    div(className := "block")(pre(snippet.code)),
  )

  private def viewSnippetPlaceholder = viewGeneric(
    header(
      text("Scala."),
      viewSlug(Slug("x" * 10 /* TODO magic number */ )),
      text(".beauty"),
    )
  )

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = {
    case Msg.GotSnippets(data) =>
      // important: this message is only relevant at home
      (model.mapPage(_.mapHome(_.copy(data = data))), Cmd.None)

    case Msg.OpenSnippet(id) =>
      (
        model.copy(page = Page.Snippet(None)),
        Cmd.emit(Msg.SetTitle("Scala.beauty - loading snippet " + id.hashed))
          |+| Cmd
            .Run(ScalaBeautyApi[IO].getSnippet(id))
            .map(output => Msg.OpenedSnippet(output.snippet)),
      )

    case Msg.OpenedSnippet(snippet) =>
      (
        model.copy(page = Page.Snippet(Some(snippet))),
        Cmd.emit(Msg.SetTitle("Scala.beauty - snippet " + snippet.id.hashed)),
      )

    case Msg.NavigateTo(externalUrl) => (model, Nav.loadUrl(externalUrl))

    case Msg.NewTab(url) =>
      (
        model,
        // Workaround for Tyrian capturing clicks on anchors
        // even if they're new tabs
        Cmd.SideEffect {
          org.scalajs.dom.window.open(url, "_blank").focus()
        },
      )

    case Msg.GoHome           => initialize
    case Msg.GoHomeResetState => initialize.fmap(_ |+| Nav.pushUrl("/"))
    case Msg.SetTitle(title) =>
      (
        model,
        Cmd.SideEffect {
          org.scalajs.dom.document.title = title
        },
      )

    case Msg.NoOp => (model, Cmd.None)
  }

  def subscriptions(model: Model): Sub[IO, Msg] = Sub.None

  def router: Location => Msg = {
    case loc: Location.External =>
      Msg.NavigateTo(loc.href)

    case loc: Location.Internal =>
      loc.pathName.match {
        case s"/snippet/${id}"  => Msg.OpenSnippet(Slug(id))
        case s"/snippet/${id}/" => Msg.OpenSnippet(Slug(id))
        case "/" | ""           => Msg.GoHome
        case _                  => Msg.GoHomeResetState
      }
  }
}
