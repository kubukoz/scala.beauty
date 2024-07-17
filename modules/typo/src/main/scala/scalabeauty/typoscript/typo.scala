// adapt to your instance and credentials
implicit val c: java.sql.Connection =
  java.sql.DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres?user=postgres&password=postgres")

import typo.*
val options = Options(
  // customize package name for generated code
  pkg = "org.foo.generated",
  // pick your database library
  dbLib = Some(DbLibName.Doobie),
  jsonLibs = Nil,
  enableDsl = true,
  // many more possibilities for customization here
  // ...
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
    options,
    targetFolder = targetDir,
    testTargetFolder = Some(testTargetDir),
    selector = selector,
    scriptsPaths = List(scriptsFolder),
  ).overwriteFolder()
