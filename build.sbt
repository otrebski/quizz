// *****************************************************************************
// Projects
// *****************************************************************************

name := "quizz"
version := "0.2"

lazy val SeleniumTest = config("selenium") extend Test

lazy val quizz =
  project
    .in(file("."))
    .enablePlugins(JavaAppPackaging)
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
          library.scalaCheck % Test,
          library.scalaTest  % Test,
          library.circeParser,
          library.circeGeneric,
          library.tapir,
          library.tapirAkka,
          library.tapirJson,
          library.sttpClient,
          library.sttpClientCirce,
          library.tapirJsonCirce,
          library.scalaLogging,
          library.logback,
          library.doobieCore,
          library.doobiePostgres,
          library.doobieQuill,
          library.doobieScalatest % Test,
//        library.tapirHttp4s,
//        library.tapirJson,
//        library.bazelServer,
//        library.bazelClient
          library.betterFiles
        )
    )
    .configs(SeleniumTest)
    .settings(
      inConfig(SeleniumTest)(Defaults.testSettings),
      libraryDependencies += library.scalaTest % SeleniumTest,
      libraryDependencies += "org.seleniumhq.selenium" % "selenium-java" % "3.141.59" % SeleniumTest,
      libraryDependencies += "org.fluentlenium" %  "fluentlenium-junit" % "3.10.1" % SeleniumTest,
      libraryDependencies += "org.fluentlenium" %  "fluentlenium-assertj" % "3.10.1" % SeleniumTest,
      libraryDependencies += "org.seleniumhq.selenium" %"selenium-chrome-driver" % "3.141.59" % SeleniumTest,
      libraryDependencies += "com.mashape.unirest" % "unirest-java" % "1.4.9" % SeleniumTest


    )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val scalaCheck = "1.14.1"
      val scalaTest  = "3.2.0"
      val circe      = "0.12.3"
      val tapir      = "0.16.16"
//      val bazel = "0.20.0"
      val scalaLogging = "3.9.2"
      val logback      = "1.2.3"
      val doobie       = "0.9.0"
      val sttp         = "2.2.8"
      val sttpTapirJsonCirce = "0.16.16"
      val betterFiles = "3.9.1"
    }
    val scalaCheck   = "org.scalacheck"         %% "scalacheck"             % Version.scalaCheck
    val scalaTest    = "org.scalatest"          %% "scalatest"              % Version.scalaTest
    val circeParser  = "io.circe"               %% "circe-parser"           % Version.circe
    val circeGeneric = "io.circe"               %% "circe-generic"          % Version.circe
    val tapir        = "com.softwaremill.sttp.tapir" %% "tapir-core"             % Version.tapir
    val tapirAkka    = "com.softwaremill.sttp.tapir" %% "tapir-akka-http-server" % Version.tapir
    val tapirJson    = "com.softwaremill.sttp.tapir" %% "tapir-json-circe"       % Version.tapir
//    val bazelClient = "org.http4s"     %% "http4s-blaze-client" % Version.bazel
//    val bazelServer = "org.http4s"     %% "http4s-blaze-server" % Version.bazel
//    val tapirHttp4s = "com.softwaremill.tapir" %% "tapir-http4s-server" % Version.tapir
    val scalaLogging    = "com.typesafe.scala-logging"   %% "scala-logging"    % Version.scalaLogging
    val sttpClient      = "com.softwaremill.sttp.client" %% "core"             % Version.sttp
    val sttpClientCirce = "com.softwaremill.sttp.client" %% "circe"            % Version.sttp
    val tapirJsonCirce  = "com.softwaremill.sttp.tapir"  %% "tapir-json-circe" % Version.sttpTapirJsonCirce
    val betterFiles     = "com.github.pathikrit" %% "better-files" % Version.betterFiles

    val logback         = "ch.qos.logback" % "logback-classic"  % Version.logback
    val doobieCore      = "org.tpolecat"  %% "doobie-core"      % Version.doobie
    val doobiePostgres  = "org.tpolecat"  %% "doobie-postgres"  % Version.doobie
    val doobieQuill     = "org.tpolecat"  %% "doobie-quill"     % Version.doobie
    val doobieScalatest = "org.tpolecat"  %% "doobie-scalatest" % Version.doobie
  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
  commonSettings ++
  scalafmtSettings

lazy val commonSettings =
  Seq(
    scalaVersion := "2.13.3",
    organization := "default",
    organizationName := "k.otrebski",
    startYear := Some(2019),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalacOptions ++= Seq(
        "-unchecked",
        "-deprecation",
        "-language:_",
        "-target:jvm-1.8",
        "-encoding",
        "UTF-8"
      ),
    Compile / unmanagedSourceDirectories := Seq((Compile / scalaSource).value),
    Test / unmanagedSourceDirectories := Seq((Test / scalaSource).value)
  )

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true
  )

mainClass := Some("quizz.web.WebApp")
