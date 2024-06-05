$version: "2"

namespace scalabeauty.api

use alloy#simpleRestJson

@simpleRestJson
service ScalaBeautyApi {
    version: "v1"
    operations: [
        GetSnippets
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
