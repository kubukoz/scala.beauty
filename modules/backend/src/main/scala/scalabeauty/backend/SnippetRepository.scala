package scalabeauty.backend

import cats.effect.kernel.Resource
import cats.effect.IO
import org.typelevel.twiddles.Iso
import scalabeauty.api.*
import skunk.Session
import smithy4s.Bijection

trait SnippetRepository {
  def createTable(): IO[Unit]
  def insert(snippets: List[Snippet]): IO[Unit]
  def getAll(): IO[List[Snippet]]
}

object SnippetRepository {
  def instance(getSession: Resource[IO, Session[IO]]): SnippetRepository = {

    import skunk.implicits.*

    object codecs {
      import skunk.codec.all.*

      given [From, To](using b: Bijection[From, To]): Iso[From, To] = Iso.instance(b.to)(b.from)

      val snippet =
        (
          text.to[Slug] *:
            text *:
            text *:
            text
              .to[GithubAuthor]
              .imap(Author.github)(_.project.github.getOrElse(sys.error("oops not a github author!")))
        ).to[Snippet]
    }

    new SnippetRepository {
      def createTable(): IO[Unit] =
        getSession.use {
          _.execute(
            sql"""create table if not exists snippets (
            id text primary key,
            description text not null,
            code text not null,
            author text not null
          )""".command
          )
        }.void

      def insert(snippets: List[Snippet]): IO[Unit] =
        getSession.use {
          _.prepare(
            sql"insert into snippets (id, description, code, author) values ${codecs.snippet.values.list(snippets)}".command
          )
            .flatMap(_.execute(snippets))
        }.void

      def getAll(): IO[List[Snippet]] =
        getSession
          .use { ses =>
            ses.execute(sql"""select id, description, code, author from snippets""".query(codecs.snippet))
          }
    }
  }
}
