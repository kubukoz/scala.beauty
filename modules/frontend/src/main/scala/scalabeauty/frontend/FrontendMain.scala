package scalabeauty.frontend

import cats.effect.IO
import cats.syntax.all.*
import org.http4s.client.Client
import org.http4s.dom.FetchClientBuilder
import org.http4s.implicits.*
import scalabeauty.api
import scalabeauty.api.{Pagination as _, *}
import smithy4s.http4s.SimpleRestJsonBuilder
import smithy4s.Timestamp
import tyrian.*
import tyrian.Html.*

import scala.annotation.nowarn
import scala.scalajs.js.annotation.JSExportTopLevel

import HtmlUtils.*

enum Msg {
  case PageFetched(data: List[RichSnippet], pagination: api.Pagination)
  case NavigateTo(url: String)
  case NewTab(url: String)
  case OpenSnippet(id: Slug)
  case GoHome(page: Option[api.Page])
  case GoHomeResetState
  case SnippetFetched(snippet: RichSnippet)
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

case class RichSnippet(
    id: Slug,
    description: String,
    code: String,
    author: Author,
    createdAt: Timestamp,
    codeHtml: String,
)

object RichSnippet {
  def fromApi(base: api.Snippet, codeHtml: String): RichSnippet = RichSnippet(
    base.id,
    base.description,
    base.code,
    base.author,
    base.createdAt,
    codeHtml,
  )
}

enum Page {
  case Home(data: List[RichSnippet], pagination: Option[api.Pagination])
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

  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = initialize(page = None)

  private def attachCode(snippet: Snippet): IO[RichSnippet] =
    Shiki
      .codeToHtml(snippet.code, "scala", "catppuccin-macchiato")
      .map(RichSnippet.fromApi(snippet, _))

  private def initialize(page: Option[api.Page]): (Model, Cmd[IO, Msg]) =
    (
      Model(Page.Home(Nil, None)),
      Cmd.emit(Msg.SetTitle("Scala.beauty"))
        |+| Cmd
          .Run(
            ScalaBeautyApi[IO]
              .getSnippets(page)
              .flatMap { output =>
                output.snippets
                  .traverse(attachCode)
                  .map(Msg.PageFetched(_, output.pagination))
              }
          ),
    )

  def view(model: Model): Html[Msg] =
    model.page.match {
      case Page.Home(data, pagination) => viewHome(data, pagination)
      case Page.Snippet(SnippetState.Fetching, placeholder) =>
        viewGeneric(SnippetComponent.viewPlaceholder(placeholder))
      case Page.Snippet(SnippetState.Fetched(snip, maskSize), placeholder) =>
        viewGeneric(SnippetComponent.view(snip.copy(id = snip.id.mask(placeholder.takeRight(maskSize))))*)
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
      Footer.view,
    )

  private def viewHome(data: List[RichSnippet], pagination: Option[api.Pagination]) =
    viewGeneric(
      heading(text("Scala.beauty")),
      subtitle(
        text("Latest Scala beauties:")
      ),
      button(className := "button block")(
        a(linkAttrs("https://github.com/kubukoz/scala.beauty/issues/new/choose"))(
          "Add yours"
        )
      ),
      if (data.isEmpty) div(className := "block")(text("Loading..."))
      else
        div(
          ul(className := "block")(
            data.map { snippet =>
              li(className := "block")(
                a(href := "/snippet/" + snippet.id)(
                  SnippetComponent.viewBox(snippet)
                )
              )

            }
          ) ::
            pagination
              .map(Pagination.getPaginationBlocks(_))
              .map(Pagination.view)
              .toList
        ),
    )

  @nowarn("msg=unused")
  private def logged[A, B](f: A => B): A => B = a => {
    val result = f(a)
    println(s"Action: $a\nResult: $result")
    result
  }

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = {
    case Msg.PageFetched(data, pagination) =>
      // important: this message is only relevant at home
      (model.mapPage(_.mapHome(_.copy(data = data, pagination = Some(pagination)))), Cmd.None)

    case Msg.OpenSnippet(id) =>
      (
        model.copy(page = Page.Snippet(SnippetState.Fetching, placeholderSlug = mkSlug(10))),
        Cmd.emit(Msg.SetTitle("Scala.beauty - loading snippet " + id.hashed))
          |+| Cmd
            .Run(ScalaBeautyApi[IO].getSnippet(id).map(_.snippet).flatMap(attachCode))
            .map(Msg.SnippetFetched(_)),
      )

    case Msg.SnippetFetched(snippet) =>
      // important: this message is only relevant at snippet.
      // this is still broken if you jump between home and snippet many times fast: the older snippet may load
      // we could fix this if the state contained some requestId of some sort and it matched what we got here...
      // ...or we can try to make the fetches subscriptions instead of commands, which will allow cancelling them
      (
        model
          .mapPage(
            _.mapSnippet(s => s.copy(state = SnippetState.Fetched(snippet, maskSize = snippet.id.value.length)))
          ),
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

    // For some (probably reasonable) reason, this gets hit twice when you go directly to home.
    // TODO: check if the model is already fetching the data?
    // (or move the IO calls to subscriptions, which should hopefully do just that)
    case Msg.GoHome(page)     => initialize(page)
    case Msg.GoHomeResetState => initialize(None).fmap(_ |+| Nav.pushUrl("/"))
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
        case Page.Home(_, _) => none
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
        case "/" | "" =>
          println("search: " + loc.search)
          Msg.GoHome {
            loc.search
              .collectFirst { case s"?page=$page" =>
                page.toIntOption
              }
              .flatten
              .map(api.Page(_))
          }
        case _ => Msg.GoHomeResetState
      }
  }
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
