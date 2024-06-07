$version: "2"

namespace scalabeauty.api

use alloy#simpleRestJson

@simpleRestJson
service ScalaBeautyApi {
    version: "v1"
    operations: [
        GetSnippets
        GetSnippet
    ]
}

@http(method: "GET", uri: "/snippets")
@readonly
operation GetSnippets {
    input := {
        @httpQuery("before")
        before: Slug

        @httpQuery("page")
        page: Page
    }

    output := {
        @httpPayload
        @required
        snippets: Snippets
    }
}

@http(method: "GET", uri: "/snippets/{id}")
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

    errors: [
        SnippetNotFound
    ]
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
