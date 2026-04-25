package scalabeauty.frontend

import scalabeauty.api
import scalabeauty.api.{Pagination as _, *}

enum HomeMsg {
  case PageFetched(data: List[RichSnippet], pagination: api.Pagination)
  case OpenSnippet(id: Slug)
  case ShortenMasksHome
  case UpdatePlaceholdersHome(hashes: List[Slug])
}
