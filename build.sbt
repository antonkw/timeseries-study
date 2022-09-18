ThisBuild / version := "0.1.0-SNAPSHOT"

import Dependencies._

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / organization := "io.github.antonkw"

ThisBuild / Compile / run / mainClass := Some("io.github.antonkw.Main")
ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-language:_",
  "-unchecked",
  "-Wunused:imports,patvars,privates,locals,explicits,synthetics",
  "-Xfatal-warnings",
  "-Ymacro-annotations"
)

def dockerSettings(name: String) = List(
  Docker / packageName := s"timeseries-study-$name",
  dockerBaseImage := "jdk17-curl:latest",
  dockerExposedPorts ++= List(8080),
  makeBatScripts := Nil,
  dockerUpdateLatest := true
)

lazy val app = (project in file("./modules/core"))
  .enablePlugins(DockerPlugin)
  .enablePlugins(AshScriptPlugin)
  .settings(dockerSettings("ws"))
  .settings(
    name := "timeseries",
    libraryDependencies ++= Seq(
          compilerPlugin(CompilerPlugins.`context-applied`),
          compilerPlugin(CompilerPlugins.`kind-projector`),
          Libraries.enumeratum,
          Libraries.skunkCore,
          Libraries.skunkRefined,
          Libraries.refinedCore,
          Libraries.refinedCats,
          Libraries.cirisCore,
          Libraries.cirisRefined,
          Libraries.log4catsCore,
          Libraries.log4catsSlf4j,
          Libraries.logback,
          Libraries.tapir,
          Libraries.tapirRefined,
          Libraries.tapirSwagger,
          Libraries.tapirHttp4s,
          Libraries.http4sBlaze,
          Libraries.newtype,
          Libraries.weaverCats
        )
  )

lazy val tests = {
  (project in file("./modules/tests"))
    .configs(IntegrationTest)
    .settings(
      name := "int-tests",
      IntegrationTest / parallelExecution := false,
      Defaults.itSettings,
      testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
      libraryDependencies ++= Seq(
            Libraries.weaverCats,
            Libraries.weaverScalaCheck,
            Libraries.scalatest
          )
    )
    .dependsOn(app)
}

lazy val root =
  project
    .in(file("."))
    .settings(
      name := "timeseries-study",
      Compile / run := (app / Compile / run).evaluated
    )
    .aggregate(app, tests)
