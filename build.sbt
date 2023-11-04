lazy val scala212 = "2.12.18"
lazy val scalaLibraryVersion = "2.13.12"
lazy val supportedScalaVersions = List(scala212, scalaLibraryVersion)

lazy val disablingPublishingSettings =
  Seq(publish / skip := true, publishArtifact := false)

lazy val enablingPublishingSettings = Seq(
  publishArtifact := true, // Enable publish
  publishMavenStyle := true,
  licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/fburato/highwheel-modules")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/fburato/highwheel-modules"),
      "scm:git@github.com:fburato/highwheel-modules.git"
    )
  ),
  developers := List(
    Developer(
      id = "fburato",
      name = "Francesco Burato",
      email = "francesco.burato@gmail.com",
      url = url("https://github.com/fburato")
    )
  ),
  Test / publishArtifact := false
)

crossScalaVersions := supportedScalaVersions

lazy val hwmParent = (project in file("."))
  .disablePlugins(AssemblyPlugin)
  .aggregate(utils, model, parser, core)
  .settings(
    crossScalaVersions := Nil,
    disablingPublishingSettings
  )

lazy val core = (project in file("core"))
  .settings(
    enablingPublishingSettings,
    commonSettings,
    setName("highwheel-modules-core"),
    libraryDependencies ++= makeDependencies(dependencies.guava, dependencies.parserCombinators),
    excludeScalaAndDependencies
  )
  .dependsOn(parser)

lazy val model = (project in file("model"))
  .settings(
    commonSettings,
    enablingPublishingSettings,
    setName("highwheel-modules-model"),
    libraryDependencies ++= commonDependencies,
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n <= 12 => List("org.scala-lang" % "scala-library" % scala212)
        case _                       => List("org.scala-lang" % "scala-library" % scalaLibraryVersion)
      }
    },
    excludeScalaAndDependencies
  )
  .dependsOn(utils)

lazy val utils = (project in file("utils"))
  .settings(
    enablingPublishingSettings,
    commonSettings,
    setName("highwheel-modules-utils"),
    libraryDependencies ++= commonDependencies,
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n <= 12 => List("org.scala-lang" % "scala-library" % scala212)
        case _                       => List("org.scala-lang" % "scala-library" % scalaLibraryVersion)
      }
    },
    excludeScalaAndDependencies
  )

lazy val parser = (project in file("parser"))
  .settings(
    enablingPublishingSettings,
    commonSettings,
    setName("highwheel-modules-parser"),
    libraryDependencies ++= makeDependencies(dependencies.asm),
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n <= 12 => List("org.scala-lang" % "scala-library" % scala212)
        case _                       => List("org.scala-lang" % "scala-library" % scalaLibraryVersion)
      }
    },
    assembly / assemblyOption := (assembly / assemblyOption).value
      .withIncludeScala(includeScala = false),
    assembly / assemblyShadeRules ++= Seq(
      ShadeRule
        .rename(
          "org.objectweb.asm.**" -> "com.github.fburato.highwheelmodules.bytecodeparser.asm.@1"
        )
        .inLibrary(dependencies.asm)
        .inProject
    ),
    assembly / assemblyMergeStrategy := {
      case x @ PathList("com", "github", "fburato", "highwheelmodules", pack, _*) =>
        if (pack == "bytecodeparser") {
          val oldStrategy = (assembly / assemblyMergeStrategy).value
          oldStrategy(x)
        } else {
          MergeStrategy.discard
        }
      case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    }
  )
  .dependsOn(model)

def setName(artifactName: String) =
  Seq(name := artifactName, assembly / assemblyJarName := s"$artifactName-${version.value}.jar")

def makeDependencies(dependencies: ModuleID*): Seq[ModuleID] =
  dependencies.toSeq ++ commonDependencies

def excludeScalaAndDependencies = {
  assembly / assemblyOption := (assembly / assemblyOption).value
    .withIncludeScala(false)
    .withIncludeDependency(false)
}

lazy val commonSettings = Seq(
  crossScalaVersions := supportedScalaVersions,
  organization := "com.github.fburato",
  resolvers ++= Seq(Resolver.mavenLocal, DefaultMavenRepository)
) ++ compilerSettings

lazy val compilerSettings = Seq(
  scalacOptions ++= compilerOptions,
  compileOrder := CompileOrder.JavaThenScala,
  javacOptions ++= Seq("-source", "17", "-target", "17", "--enable-preview")
)

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding",
  "utf8",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:postfixOps",
  "-target:jvm-1.8"
)

lazy val dependencies = new {
  private val asmVersion = "9.6"
  private val guavaVersion = "31.1-jre"
  private val parserCombinatorsVersion = "2.2.0"

  val asm = "org.ow2.asm" % "asm" % asmVersion
  val guava = "com.google.guava" % "guava" % guavaVersion
  val parserCombinators =
    "org.scala-lang.modules" %% "scala-parser-combinators" % parserCombinatorsVersion
}

lazy val testDependencies = new {
  private val scalaTestVersion = "3.2.15"
  private val mockitoScalaVersion = "1.17.29"
  private val mockitoVersion = "5.7.0"
  private val apacheCommonsLangVersion = "3.13.0"

  val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
  val mockitoScalaTest = "org.mockito" %% "mockito-scala-scalatest" % mockitoScalaVersion % "test"
  val mockito = "org.mockito" % "mockito-core" % mockitoVersion % "test"
  val apacheCommonsLang = "org.apache.commons" % "commons-lang3" % apacheCommonsLangVersion % "test"
}

lazy val commonDependencies = Seq(
  testDependencies.scalaTest,
  testDependencies.mockitoScalaTest,
  testDependencies.mockito,
  testDependencies.apacheCommonsLang
)
