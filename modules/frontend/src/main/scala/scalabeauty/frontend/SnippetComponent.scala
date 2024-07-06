package scalabeauty.frontend

import cats.syntax.all.*
import scalabeauty.api
import scalabeauty.api.Author
import scalabeauty.api.Author.GithubCase
import scalabeauty.api.Slug
import smithy4s.Timestamp
import tyrian.*
import tyrian.Html.*

import HtmlUtils.*

object SnippetComponent {
  def view(snippet: RichSnippet): List[Html[Msg]] = List(
    heading(
      text("Scala."),
      viewSlug(snippet.id),
      text(".beauty"),
    ),
    subtitle(
      text(" by "),
      viewAuthor(snippet.author),
    ),
    div(className := "subtitle is-6")(
      text("on "),
      viewDate(snippet.createdAt),
    ),
    p(className := "block")(i(snippet.description)),
    div(className := "block")().innerHtml(snippet.codeHtml),
  )

  def viewBox(snippet: RichSnippet): Html[Msg] =
    div(className := "box")(
      div(className := "block is-flex is-justify-content-space-between")(
        div(
          viewSlug(snippet.id),
          text(" by "),
          viewAuthor(snippet.author),
        ),
        div(
          text("on "),
          viewDate(snippet.createdAt),
        ),
      ),
      p(className := "block")(i(snippet.description)),
      div(className := "block")().innerHtml(snippet.codeHtml),
    )

  def viewPlaceholder(slug: Slug): Html[Nothing] =
    heading(
      text("Scala."),
      viewSlug(slug),
      text(".beauty"),
    )

  private def viewSlug(slug: Slug): Html[Nothing] = span(className := "has-text-grey is-family-monospace")(slug.hashed)

  private def viewAuthor(author: Author): Html[Msg] = author.match { case GithubCase(github) =>
    val url = show"https://github.com/${github.username}"
    a(linkAttrs(url))(
      show"@${github.username}"
    )
  }
  private def viewDate(date: Timestamp) =
    span(className := "has-text-grey is-family-monospace") {
      val jsDate = date.toDate

      // e.g. 2024-07-06
      "%04d-%02d-%02d".format(
        jsDate.getUTCFullYear(),
        jsDate.getUTCMonth() + 1,
        jsDate.getUTCDate(),
      )
    }

}
