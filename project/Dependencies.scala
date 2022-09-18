import sbt._

object Dependencies {
  case object Versions {
    val catsCore   = "2.6.1"
    val catsEffect = "3.1.0"
    val newtype    = "0.4.4"
    val refined    = "0.10.1"
    val http4s     = "0.23.1"
    val circe      = "0.14.1"
    val enumeratum = "1.7.0"
    val pureConfig = "0.16.0"
    val chimney    = "0.6.1"
    val log4cats   = "2.5.0"
    val logback    = "1.4.1"
    val decline    = "2.1.0"
    val ciris      = "2.3.3"

    val skunk     = "0.3.1"
    val weaver    = "0.8.0"
    val scalatest = "3.2.11"
    val tapir     = "1.1.0"

    val kindProjector    = "0.13.2"
    val betterMonadicFor = "0.3.1"
    val contextApplied   = "0.1.4"
  }

  case object Libraries {
    private def enumeratum_(artifact: String): ModuleID = "com.beachape" %% artifact % Versions.enumeratum

    val catsCore    = typelevel("cats-core", Versions.catsCore)
    val catsEffect  = typelevel("cats-effect", Versions.catsEffect)
    val refinedCore = refined("refined")
    val refinedCats = refined("refined-cats")

    val cirisCore    = ciris("ciris")
    val cirisRefined = ciris("ciris-refined")

    val newtype    = "io.estatico" %% "newtype" % Versions.newtype
    val enumeratum = enumeratum_("enumeratum")

    val logback       = "ch.qos.logback" % "logback-classic" % Versions.logback
    val log4catsSlf4j = typelevel("log4cats-slf4j", Versions.log4cats)
    val log4catsCore  = typelevel("log4cats-core", Versions.log4cats)

    val skunkCore    = skunk("skunk-core")
    val skunkRefined = skunk("refined")

    val weaverCats       = weaver("weaver-cats")
    val weaverScalaCheck = weaver("weaver-scalacheck")
    val weaverDiscipline = weaver("weaver-discipline")
    val scalatest        = "org.scalatest" %% "scalatest" % Versions.scalatest % Test

    val tapir        = "com.softwaremill.sttp.tapir" %% "tapir-core"              % Versions.tapir
    val tapirHttp4s  = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"     % Versions.tapir
    val http4sBlaze  = "org.http4s"                  %% "http4s-blaze-server"     % "0.23.12"
    val tapirSwagger = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % Versions.tapir
    val tapirRefined = "com.softwaremill.sttp.tapir" %% "tapir-refined"           % Versions.tapir

    def skunk: String => ModuleID = tpolecat(Versions.skunk)

    def tpolecat(version: String)(artifact: String) = "org.tpolecat" %% artifact % version

    def weaver(artifact: String): ModuleID = "com.disneystreaming" %% artifact % Versions.weaver

    def typelevel(artifact: String, version: String): ModuleID = "org.typelevel" %% artifact % version

    def refined(artifact: String): ModuleID = "eu.timepit" %% artifact % Versions.refined

    def circe(artifact: String): ModuleID = "io.circe" %% artifact % Versions.circe

    def http4s(artifact: String): ModuleID = "org.http4s" %% artifact % Versions.http4s

    def ciris(artifact: String): ModuleID = "is.cir" %% artifact % Versions.ciris

  }

  case object CompilerPlugins {
    val `kind-projector`     = "org.typelevel"  %% "kind-projector"     % Versions.kindProjector cross CrossVersion.full
    val `context-applied`    = "org.augustjune" %% "context-applied"    % Versions.contextApplied
    val `better-monadic-for` = "com.olegpy"     %% "better-monadic-for" % Versions.betterMonadicFor
  }
}
