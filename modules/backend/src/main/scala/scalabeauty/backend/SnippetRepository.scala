package scalabeauty.backend

import cats.effect.kernel.Resource
import cats.effect.IO
import org.typelevel.twiddles.Iso
import scalabeauty.api.*
import skunk.Codec
import skunk.Session
import smithy4s.Bijection
import smithy4s.Timestamp

import java.time.Instant
import java.time.ZoneOffset

trait SnippetRepository {
  def createTable(): IO[Unit]
  def insert(snippets: List[Snippet]): IO[Unit]
  def getAll(offset: Long, limit: Int): IO[List[Snippet]]
  def countAll(): IO[Long]
  def get(id: Slug): IO[Option[Snippet]]
}

object SnippetRepository {
  def instance(getSession: Resource[IO, Session[IO]]): SnippetRepository = {

    import skunk.implicits.*

    object codecs {
      import skunk.codec.all.*
      export skunk.codec.all.int8
      import skunk.smithy4s.codec.all.*

      given [From, To](using b: Bijection[From, To]): Iso[From, To] = Iso.instance(b.to)(b.from)

      val instant: Codec[Instant] =
        timestamptz.imap(_.toInstant())(_.atOffset(ZoneOffset.UTC))

      val timestamp: Codec[Timestamp] =
        instant.imap(Timestamp.fromInstant)(_.toInstant)

      val slug: Codec[Slug] = text.to[Slug]

      val snippet: Codec[Snippet] = {
        slug *:
          text *:
          text *:
          jsonb[Author] *:
          timestamp
      }.to[Snippet]
    }

    new SnippetRepository {
      private val allFields = sql"""id, description, code, author, created_at"""

      def createTable(): IO[Unit] =
        getSession.use { pool =>
          pool.execute(sql"""create table if not exists snippets (
              id text primary key,
              description text not null,
              code text not null,
              author jsonb not null,
              created_at timestamptz not null
            )""".command) *>
            pool.execute(
              sql"create index if not exists snipets_created_at on snippets(created_at)".command
            )
        }.void

      def insert(snippets: List[Snippet]): IO[Unit] =
        getSession.use {
          _.prepare(
            sql"""insert into snippets ($allFields) values ${codecs.snippet.values.list(snippets)}
                 |on conflict(id) do update set
                 |description = EXCLUDED.description,
                 |code = EXCLUDED.code,
                 |author = EXCLUDED.author,
                 |created_at = EXCLUDED.created_at""".stripMargin.command
          )
            .flatMap(_.execute(snippets))
        }.void

      def getAll(offset: Long, limit: Int): IO[List[Snippet]] =
        getSession
          .use(
            _.execute(
              sql"""select $allFields from snippets order by created_at desc offset ${codecs.int8} limit ${codecs.int8}"""
                .query(
                  codecs.snippet
                )
            )((offset, limit))
          )

      def countAll(): IO[Long] =
        getSession.use(_.unique(sql"""select count(*) from snippets""".query(codecs.int8)))

      def get(id: Slug): IO[Option[Snippet]] =
        getSession.use { ses =>
          ses
            .prepare(
              sql"""select $allFields from snippets where id = ${codecs.slug}"""
                .query(codecs.snippet)
            )
            .flatMap(_.option(id))
        }
    }
  }

  val inMemory: IO[SnippetRepository] = IO.ref(Map.empty[Slug, Snippet]).map { ref =>
    new SnippetRepository {
      def createTable(): IO[Unit] = IO.unit

      def insert(snippets: List[Snippet]): IO[Unit] = ref.update { old =>
        snippets.foldLeft(old) { (acc, snippet) =>
          acc.updated(snippet.id, snippet)
        }
      }

      def getAll(offset: Long, limit: Int): IO[List[Snippet]] =
        ref.get.map(
          _.values.toList
            .sortBy(_.createdAt.toInstant)
            .reverse
            .drop(offset.toInt)
            .take(limit)
        )

      def countAll(): IO[Long] = ref.get.map(_.size.toLong)

      def get(id: Slug): IO[Option[Snippet]] = ref.get.map(_.get(id))
    }
  }

}
