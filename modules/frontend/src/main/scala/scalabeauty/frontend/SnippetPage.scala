package scalabeauty.frontend

import scalabeauty.api.*

enum SnippetMsg {
  case SnippetFetched(snippet: RichSnippet)
  case UpdatePlaceholder(hash: Slug)
  case ShortenMask
}
