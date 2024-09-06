import typo.*

import java.sql.DriverManager

val conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "postgres")

val init =
  try
    conn
      .createStatement()
      .execute("""
create table if not exists snippets (
id text primary key,
description text not null,
code text not null,
author jsonb not null,
created_at timestamptz not null);""")
  finally conn.close()

val ds = typo.TypoDataSource.hikari(
  server = "localhost",
  port = 5432,
  databaseName = "postgres",
  username = "postgres",
  password = "postgres",
)

val options = Options(
  pkg = "scalabeauty.db.generated",
  dbLib = Some(DbLibName.Doobie),
  jsonLibs = Nil,
  enableDsl = true,
)

// current folder, where you run the script from
val location = java.nio.file.Path.of(sys.props("user.dir")).getParent().getParent()

// destination folder. All files in this dir will be overwritten!
val targetDir     = location.resolve("modules/typoModels/src/main/scala/scalabeauty/backend/db/generated")
val testTargetDir = location.resolve("modules/typoModels/src/test/scala/scalabeauty/backend/db/generated")

// where Typo will look for sql files
val scriptsFolder = location.resolve("sql")

// you can use this to customize which relations you want to generate code for, see below
val selector = Selector.ExcludePostgresInternal

@main def run =
  generateFromDb(
    ds,
    options,
    targetFolder = targetDir,
    testTargetFolder = Some(testTargetDir),
    selector = selector,
    scriptsPaths = List(scriptsFolder),
  ).foreach(_.overwriteFolder())
