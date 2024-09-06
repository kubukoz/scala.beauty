$version: "2"

namespace scalabeauty.api

use alloy#simpleRestJson

@simpleRestJson
service ScalaBeautyApi {
    version: "v1"
    operations: [GetSnippets, GetSnippet]
}

@http(method: "GET", uri: "/api/snippets")
@readonly
operation GetSnippets {
    input := {
        @httpQuery("page")
        page: Page
    }
    output := {
        @required
        snippets: Snippets
        @required
        pagination: Pagination
    }
}

structure Pagination {
    @required
    currentPage: Page
    @required
    totalPages: Page
}

@http(method: "GET", uri: "/api/snippets/{id}")
@readonly
operation GetSnippet {
    input := {
        @httpLabel
        @required
        id: Slug
    }
    output := {
        @httpPayload
        @required
        snippet: Snippet
    }
    errors: [SnippetNotFound]
}

@error("client")
@httpError(404)
structure SnippetNotFound {}

list Snippets {
    member: Snippet
}

structure Snippet {
    @required
    id: Slug
    @required
    description: String
    @required
    code: String
    @required
    author: Author
    @required
    @timestampFormat("date-time")
    createdAt: Timestamp
}

union Author {
    github: GithubAuthor
}

structure GithubAuthor {
    @required
    username: String
}

string Slug

@range(min: 0)
integer Page
