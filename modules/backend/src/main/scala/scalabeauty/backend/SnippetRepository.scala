package scalabeauty.backend

import cats.effect.IO
import doobie.implicits.*
import doobie.util.transactor.Transactor
import scalabeauty.api.*
import scalabeauty.db.generated.customtypes.TypoInstant
import scalabeauty.db.generated.customtypes.TypoJsonb
import scalabeauty.db.generated.init.InitSqlRepo
import scalabeauty.db.generated.init.InitSqlRepoImpl
import scalabeauty.db.generated.public.snippets.SnippetsId
import scalabeauty.db.generated.public.snippets.SnippetsRepo
import scalabeauty.db.generated.public.snippets.SnippetsRepoImpl
import scalabeauty.db.generated.public.snippets.SnippetsRow
import smithy4s.json.Json
import smithy4s.Blob
import smithy4s.Timestamp

trait SnippetRepository {
  def createTable(): IO[Unit]
  def insert(snippets: List[Snippet]): IO[Unit]
  def getAll(offset: Int, limit: Int): IO[List[Snippet]]
  def countAll(): IO[Int]
  def get(id: Slug): IO[Option[Snippet]]
}

object SnippetRepository {

  def doobieInstance(xa: Transactor[IO]): SnippetRepository = new {
    private val underlyingInit: InitSqlRepo = new InitSqlRepoImpl()
    private val underlying: SnippetsRepo    = new SnippetsRepoImpl()

    def countAll(): IO[Int]     = underlying.select.count.transact(xa)
    def createTable(): IO[Unit] = underlyingInit().transact(xa).void
    def get(id: Slug): IO[Option[Snippet]] = underlying
      .selectById(SnippetsId(id.value))
      .map(_.map(decodeSnippet))
      .transact(xa)

    private def decodeSnippet(s: SnippetsRow): Snippet = Snippet(
      id = Slug(s.id.value),
      description = s.description,
      code = s.code,
      author = Json.read[Author](Blob(s.author.value)).toTry.get,
      createdAt = Timestamp.fromInstant(s.createdAt.value),
    )

    private def encodeSnippet(s: Snippet): SnippetsRow = SnippetsRow(
      id = SnippetsId(s.id.value),
      description = s.description,
      code = s.code,
      author = TypoJsonb(Json.writeBlob(s.author).toUTF8String),
      createdAt = TypoInstant(s.createdAt.toInstant),
    )

    def getAll(offset: Int, limit: Int): IO[List[Snippet]] =
      underlying.select
        .orderBy(_.createdAt.desc)
        .offset(offset)
        .limit(limit)
        .toList
        .transact(xa)
        .map(_.map(decodeSnippet))

    def insert(snippets: List[Snippet]): IO[Unit] =
      underlying
        .upsertBatch(snippets.map(encodeSnippet))
        .compile
        .drain
        .transact(xa)
  }

  val inMemory: IO[SnippetRepository] = IO.ref(Map.empty[Slug, Snippet]).map { ref =>
    new SnippetRepository {
      def createTable(): IO[Unit] = IO.unit

      def insert(snippets: List[Snippet]): IO[Unit] = ref.update { old =>
        snippets.foldLeft(old) { (acc, snippet) =>
          acc.updated(snippet.id, snippet)
        }
      }

      def getAll(offset: Int, limit: Int): IO[List[Snippet]] =
        ref.get.map(
          _.values.toList
            .sortBy(_.createdAt.toInstant)
            .reverse
            .drop(offset)
            .take(limit)
        )

      def countAll(): IO[Int] = ref.get.map(_.size)

      def get(id: Slug): IO[Option[Snippet]] = ref.get.map(_.get(id))
    }
  }

}
