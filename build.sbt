lazy val root = (project in file("."))
  .disablePlugins(AssemblyPlugin).aggregate(
  utils,
  model,
  parser,
  core
)

lazy val core = (project in file("core"))
  .settings(commonSettings ++ Seq(
    name := "highwheel-modules-core",
    libraryDependencies ++= Seq(
      dependencies.guava,
      dependencies.parserCombinators
    ) ++ commonDependencies
  ))
  .dependsOn(
    parser
  )

lazy val model = (project in file("model"))
  .settings(commonSettings ++ Seq(
    name := "highwheel-modules-model",
    libraryDependencies ++= commonDependencies
  ))
  .dependsOn(
    utils
  )

lazy val utils = (project in file("utils"))
  .settings(commonSettings ++ Seq(
    name := "highwheel-modules-utils",
    libraryDependencies ++= commonDependencies
  ))

lazy val parser = (project in file("parser"))
  .settings(commonSettings ++ Seq(
    name := "highwheel-modules-parser",
    libraryDependencies ++= Seq(
      dependencies.asm
    ) ++ commonDependencies
  ))
  .dependsOn(
    model
  )


lazy val commonSettings = Seq(
  scalaVersion := "2.13.3",
  organization := "com.github.fburato",
  resolvers ++= Seq(
    Resolver.mavenLocal,
    DefaultMavenRepository
  )
) ++ compilerSettings


lazy val compilerSettings = Seq(
  scalacOptions ++= compilerOptions,
  javacOptions in (Compile, compile) ++= Seq("-source", "1.8", "-target", "1.8")
)

lazy val compilerOptions = Seq(
  "-encoding", "utf8",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:postfixOps"
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
  testDependencies.apacheCommonsLang
)