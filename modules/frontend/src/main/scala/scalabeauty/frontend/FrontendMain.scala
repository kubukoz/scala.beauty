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
import smithy4s.Timestamp
import tyrian.*
import tyrian.Html.*

import scala.annotation.nowarn
import scala.scalajs.js.annotation.JSExportTopLevel

enum Msg {
  // todo rename
  case GotSnippets(data: List[RichSnippet])
  case NavigateTo(url: String)
  case NewTab(url: String)
  case OpenSnippet(id: Slug)
  case GoHome
  case GoHomeResetState
  // todo rename
  case OpenedSnippet(snippet: RichSnippet)
  case NoOp
  case SetTitle(title: String)
  case UpdatePlaceholder(hash: Slug)
  case ShortenMask
}

case class Model(page: Page) {
  def mapPage(f: Page => Page): Model = copy(page = f(page))
}

enum SnippetState {
  case Fetching
  case Fetched(snippet: RichSnippet, maskSize: Int)

  def mapFetched(f: SnippetState.Fetched => SnippetState.Fetched): SnippetState = this match {
    case fe: SnippetState.Fetched => f(fe)
    case s                        => s
  }
}

case class RichSnippet(base: api.Snippet, codeHtml: String)
enum Page {
  case Home(data: List[RichSnippet])
  case Snippet(state: SnippetState, placeholderSlug: Slug)

  def mapHome(f: Page.Home => Page.Home): Page = this match {
    case h: Home    => f(h)
    case s: Snippet => s
  }

  def mapSnippet(f: Page.Snippet => Page.Snippet): Page = this match {
    case h: Home    => h
    case s: Snippet => f(s)
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

  private def attachCode(snippet: Snippet): IO[RichSnippet] =
    IO.fromPromise {
      IO {
        Shiki
          .codeToHtml(
            snippet.code,
            scalajs.js.Dynamic.literal(
              lang = "scala",
              theme = "catppuccin-macchiato",
            ),
          )
      }
    }.map(RichSnippet(snippet, _))

  private def initialize: (Model, Cmd[IO, Msg]) =
    (
      Model(Page.Home(Nil)),
      Cmd.emit(Msg.SetTitle("Scala.beauty"))
        |+| Cmd
          .Run(ScalaBeautyApi[IO].getSnippets(None).flatMap { output =>
            output.snippets
              .traverse(attachCode)
          })
          .map(out => Msg.GotSnippets(out)),
    )

  def view(model: Model): Html[Msg] =
    model.page.match {
      case Page.Home(data)                                                 => viewHome(data)
      case Page.Snippet(SnippetState.Fetching, placeholder)                => viewSnippetPlaceholder(placeholder)
      case Page.Snippet(SnippetState.Fetched(snip, maskSize), placeholder) =>
        // in desperate need of lenses... or better code
        viewSnippet(snip.copy(base = snip.base.copy(id = snip.base.id.mask(placeholder.takeRight(maskSize)))))
    }

  private def viewGeneric(content: Elem[Msg]*) =
    div(className := "hero is-fullheight")(
      section(
        className := "section"
      )(
        div(
          className := "container"
        )(content.toList)
      ),
      viewFooter,
    )

  private def viewHome(data: List[RichSnippet]) =
    viewGeneric(
      header(text("Scala.beauty")),
      subtitle(
        text("Latest Scala beauties:")
      ),
      button(className := "button block")(
        a(href := "https://github.com/kubukoz/scala.beauty/issues/new/choose", target := "_blank")(
          "Add yours"
        )
      ),
      if (data.isEmpty) div(className := "block")(text("Loading..."))
      else
        div(
          ul(className := "block")(
            data.map { snippet =>
              li(className := "block")(
                a(href := "/snippet/" + snippet.base.id)(
                  viewSnippetBox(snippet)
                )
              )

            }
          ),
          viewPagination,
        ),
    )

  private def viewSnippetBox(snippet: RichSnippet) =
    div(className := "box")(
      div(className := "block is-flex is-justify-content-space-between")(
        div(
          viewSlug(snippet.base.id),
          text(" by "),
          viewAuthor(snippet.base.author),
        ),
        div(
          text("on "),
          viewDate(snippet.base.createdAt),
        ),
      ),
      p(className := "block")(i(snippet.base.description)),
      div(className := "block")().innerHtml(snippet.codeHtml),
    )

  private def viewPagination =
    nav(
      className := "pagination block is-centered",
      role      := "navigation",
    )(
      ul(className := "pagination-list")(
        li(
          a(className := "pagination-link")("1")
        ),
        li(
          span(className := "pagination-ellipsis")("…")
        ),
        li(
          a(className := "pagination-link")("45")
        ),
        li(
          a(
            className := "pagination-link is-current"
          )("46")
        ),
        li(
          a(className := "pagination-link")("47")
        ),
        li(
          span(className := "pagination-ellipsis")("…")
        ),
        li(
          a(className := "pagination-link")("86")
        ),
      )
    )

  private def viewFooter =
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

  def viewSlug(slug: Slug) = span(className := "has-text-grey is-family-monospace")(slug.hashed)

  def viewDate(date: Timestamp) =
    span(className := "has-text-grey is-family-monospace") {
      val jsDate = date.toDate

      // e.g. 2024-07-06
      "%04d-%02d-%02d".format(
        jsDate.getUTCFullYear(),
        jsDate.getUTCMonth() + 1,
        jsDate.getUTCDate(),
      )
    }

  extension (s: Slug) {
    def hashed: String          = "#" + s.value
    def nonEmpty: Boolean       = s.value.nonEmpty
    def takeRight(n: Int): Slug = Slug(s.value.takeRight(n))
    def mask(another: Slug): Slug = {
      val n = another.value.length
      Slug(s.value.dropRight(n) + another.value)
    }
  }

  private def header(items: Elem[Msg]*) =
    h1(
      className := "title"
    )(items.toList)

  private def subtitle(items: Elem[Msg]*) =
    p(
      className := "subtitle"
    )(items.toList)

  private def linkAttrs(url: String) = List(
    href := url,
    onClick(Msg.NewTab(url)),
  )

  private def viewAuthor(author: Author) = author.match { case GithubCase(github) =>
    val url = show"https://github.com/${github.username}"
    a(linkAttrs(url))(
      show"@${github.username}"
    )
  }

  private def viewSnippet(snippet: RichSnippet) = viewGeneric(
    header(
      text("Scala."),
      viewSlug(snippet.base.id),
      text(".beauty"),
    ),
    subtitle(
      text(" by "),
      viewAuthor(snippet.base.author),
    ),
    div(className := "subtitle is-6")(
      text("on "),
      viewDate(snippet.base.createdAt),
    ),
    p(className := "block")(i(snippet.base.description)),
    div(className := "block")().innerHtml(snippet.codeHtml),
  )

  private def viewSnippetPlaceholder(slug: Slug) = viewGeneric(
    header(
      text("Scala."),
      viewSlug(slug),
      text(".beauty"),
    )
  )

  @nowarn("msg=unused")
  private def logged[A, B](f: A => B): A => B = a => {
    val result = f(a)
    println(s"Action: $a\nResult: $result")
    result
  }

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = {
    case Msg.GotSnippets(data) =>
      // important: this message is only relevant at home
      (model.mapPage(_.mapHome(_.copy(data = data))), Cmd.None)

    case Msg.OpenSnippet(id) =>
      (
        model.copy(page = Page.Snippet(SnippetState.Fetching, placeholderSlug = mkSlug(10))),
        Cmd.emit(Msg.SetTitle("Scala.beauty - loading snippet " + id.hashed))
          |+| Cmd
            .Run(ScalaBeautyApi[IO].getSnippet(id).map(_.snippet).flatMap(attachCode))
            .map(Msg.OpenedSnippet(_)),
      )

    case Msg.OpenedSnippet(snippet) =>
      // important: this message is only relevant at snippet.
      // this is still broken if you jump between home and snippet many times fast: the older snippet may load
      // we could fix this if the state contained some requestId of some sort and it matched what we got here...
      // ...or we can try to make the fetches subscriptions instead of commands, which will allow cancelling them
      (
        model
          .mapPage(
            _.mapSnippet(s => s.copy(state = SnippetState.Fetched(snippet, maskSize = snippet.base.id.value.length)))
          ),
        Cmd.emit(Msg.SetTitle("Scala.beauty - snippet " + snippet.base.id.hashed)),
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

    // For some (probably reasonable) reason, this gets hit twice when you go directly to home.
    // TODO: check if the model is already fetching the data?
    // (or move the IO calls to subscriptions, which should hopefully do just that)
    case Msg.GoHome           => initialize
    case Msg.GoHomeResetState => initialize.fmap(_ |+| Nav.pushUrl("/"))
    case Msg.SetTitle(title) =>
      (
        model,
        Cmd.SideEffect {
          org.scalajs.dom.document.title = title
        },
      )
    case Msg.UpdatePlaceholder(hash) =>
      // I really need lenses... and some OOP
      model.mapPage(
        _.mapSnippet(s => s.copy(placeholderSlug = hash))
      ) -> Cmd.None
    case Msg.ShortenMask =>
      model.mapPage(
        _.mapSnippet(s => s.copy(state = s.state.mapFetched(s => s.copy(maskSize = s.maskSize - 1))))
      ) -> Cmd.None

    case Msg.NoOp => (model, Cmd.None)
  }

  def subscriptions(model: Model): Sub[IO, Msg] =
    model.page
      .match {
        case Page.Snippet(state, _) =>
          state.match {
            case SnippetState.Fetching         => Masked.Model.Pending
            case SnippetState.Fetched(_, size) => Masked.Model.Fetched(size)
          }.some
        case Page.Home(data) => none
      }
      .foldMap(
        Masked.subscriptions(_)(
          onShorten = Msg.ShortenMask,
          onUpdate = Msg.UpdatePlaceholder(mkSlug(10)),
        )
      )

  // todo share with backend
  // todo side effects
  private def mkSlug(len: Int) = Slug(scala.util.Random.alphanumeric.take(len).mkString.toLowerCase())

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
