import sbtrelease.ReleaseStateTransformations._
val scalaLibraryVersion = "2.13.3"

lazy val disablingPublishingSettings =
  Seq(skip in publish := true, publishArtifact := false)

lazy val enablingPublishingSettings = Seq(
  publishArtifact := true, // Enable publish
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),

  homepage := Some(url("https://github.com/fburato/highwheel-modules")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/fburato/highwheel-modules"),
      "scm:git@github.com:fburato/highwheel-modules.git"
    )),
  developers := List(
    Developer(id="fburato", name="Francesco Burato", email="francesco.burato@gmail.com", url=url("https://github.com/fburato"))
  ),
  publishArtifact in Test := false,
)

lazy val hwmParent = (project in file("."))
  .disablePlugins(AssemblyPlugin)
  .aggregate(
    utils,
    model,
    parser,
    core
  )
  .settings(
    disablingPublishingSettings,
    releaseProcess := Seq[ReleaseStep] (
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommand("publishM2"),
      releaseStepCommand("publishSigned"),
      releaseStepCommand("sonatypeRelease"),
      setNextVersion,
      commitNextVersion,
      runClean,
      runTest,
      releaseStepCommand("publishM2"),
      releaseStepCommand("publishSigned"),
      pushChanges
    )
  )

lazy val core = (project in file("core"))
  .settings(
    enablingPublishingSettings,
    commonSettings,
    setName("highwheel-modules-core"),
    libraryDependencies ++= makeDependencies(
      dependencies.guava,
      dependencies.parserCombinators
    ),
    excludeScalaAndDependencies
  )
  .dependsOn(
    parser
  )

lazy val model = (project in file("model"))
  .settings(commonSettings,
    enablingPublishingSettings,
    setName("highwheel-modules-model"),
    libraryDependencies ++= commonDependencies,
    excludeScalaAndDependencies
  )
  .dependsOn(
    utils
  )

lazy val utils = (project in file("utils"))
  .settings(
    enablingPublishingSettings,
    commonSettings,
    setName("highwheel-modules-utils"),
    libraryDependencies ++= commonDependencies,
    excludeScalaAndDependencies
  )

lazy val parser = (project in file("parser"))
  .settings(
    enablingPublishingSettings,
    commonSettings,
    setName("highwheel-modules-parser"),
    libraryDependencies ++= makeDependencies(
      dependencies.asm
    ),
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(
      includeScala = false
    ),
    assemblyShadeRules in assembly ++= Seq(
      ShadeRule.rename("org.objectweb.asm.**" -> "com.github.fburato.highwheelmodules.bytecodeparser.asm.@1")
        .inLibrary(dependencies.asm)
        .inProject
    ),
    assemblyMergeStrategy in assembly := {
      case x @ PathList("com", "github", "fburato", "highwheelmodules", pack, _*) =>
        if(pack == "bytecodeparser") {
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
        } else {
          MergeStrategy.discard
        }
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )
  .dependsOn(
    model
  )

def setName(artifactName: String) =
  Seq(
    name := artifactName,
    assemblyJarName in assembly := s"$artifactName-${version.value}.jar"
  )

def makeDependencies(dependencies: ModuleID*): Seq[ModuleID] =
  dependencies.toSeq ++ commonDependencies

def excludeScalaAndDependencies = {
  assemblyOption in assembly := (assemblyOption in assembly).value.copy(
    includeScala = false,
    includeDependency = false
  )
}

lazy val commonSettings = Seq(
  scalaVersion := scalaLibraryVersion,
  organization := "com.github.fburato",
  resolvers ++= Seq(
    Resolver.mavenLocal,
    DefaultMavenRepository
  )
) ++ compilerSettings


lazy val compilerSettings = Seq(
  scalacOptions ++= compilerOptions,
  compileOrder := CompileOrder.JavaThenScala,
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
)

lazy val compilerOptions = Seq(
  "-encoding", "utf8",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:postfixOps",
  "-target:jvm-1.8"
)

lazy val dependencies = new {
  private val asmVersion = "8.0.1"
  private val guavaVersion = "30.1-jre"
  private val parserCombinatorsVersion = "1.1.2"

  val asm = "org.ow2.asm" % "asm" % asmVersion
  val guava = "com.google.guava" % "guava" % guavaVersion
  val parserCombinators = "org.scala-lang.modules" %% "scala-parser-combinators" % parserCombinatorsVersion
}

lazy val testDependencies = new {
  private val scalaTestVersion = "3.1.1"
  private val mockitoScalaVersion = "1.15.0"
  private val mockitoVersion = "3.5.2"
  private val apacheCommonsLangVersion = "3.11"

  val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
  val mockitoScalaTest = "org.mockito" %% "mockito-scala-scalatest" % mockitoScalaVersion % "test"
  val mockito = "org.mockito" % "mockito-core" % mockitoVersion % "test"
  val apacheCommonsLang = "org.apache.commons" % "commons-lang3" % apacheCommonsLangVersion % "test"
}

lazy val commonDependencies = Seq(
  testDependencies.scalaTest,
  testDependencies.mockitoScalaTest,
  testDependencies.mockito,
  testDependencies.apacheCommonsLang,
  "org.scala-lang" % "scala-library" % scalaLibraryVersion
)