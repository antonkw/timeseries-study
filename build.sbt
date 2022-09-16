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

lazy val root = (project in file("."))
  .settings(
    name := "timeseries",
    libraryDependencies ++= Seq(
          compilerPlugin(CompilerPlugins.`context-applied`),
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
          Libraries.newtype
        )
  )
