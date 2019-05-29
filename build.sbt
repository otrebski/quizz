// *****************************************************************************
// Projects
// *****************************************************************************

name := "quizz"
version := "0.1"

lazy val quizz =
  project
    .in(file("."))
//    .enablePlugins(AutomateHeaderPlugin)
    .enablePlugins(JavaAppPackaging)
    .enablePlugins(DockerPlugin)
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
        library.scalaLogging,
        library.logback
//        library.tapirHttp4s,
//        library.tapirJson,
//        library.bazelServer,
//        library.bazelClient
      )
    )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val scalaCheck = "1.14.0"
      val scalaTest  = "3.0.6"
      val circe = "0.11.1"
      val tapir = "0.7.10"
//      val bazel = "0.20.0"
      val scalaLogging = "3.9.2"
      val logback = "1.2.3"
    }
    val scalaCheck = "org.scalacheck" %% "scalacheck" % Version.scalaCheck
    val scalaTest  = "org.scalatest"  %% "scalatest"  % Version.scalaTest
    val circeParser = "io.circe" %% "circe-parser" %  Version.circe
    val circeGeneric =  "io.circe" %% "circe-generic" % Version.circe
    val tapir = "com.softwaremill.tapir" %% "tapir-core" % Version.tapir
    val tapirAkka = "com.softwaremill.tapir" %% "tapir-akka-http-server" % Version.tapir
    val tapirJson = "com.softwaremill.tapir" %% "tapir-json-circe" % Version.tapir
//    val bazelClient = "org.http4s"     %% "http4s-blaze-client" % Version.bazel
//    val bazelServer = "org.http4s"     %% "http4s-blaze-server" % Version.bazel
//    val tapirHttp4s = "com.softwaremill.tapir" %% "tapir-http4s-server" % Version.tapir
    val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % Version.scalaLogging
    val logback = "ch.qos.logback" % "logback-classic" % Version.logback
  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
  commonSettings ++
  scalafmtSettings

lazy val commonSettings =
  Seq(
    scalaVersion := "2.12.8",
    organization := "default",
    organizationName := "k.otrebski",
    startYear := Some(2019),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-target:jvm-1.8",
      "-encoding", "UTF-8",
      "-Ypartial-unification",
      "-Ywarn-unused-import",
    ),
    Compile / unmanagedSourceDirectories := Seq((Compile / scalaSource).value),
    Test / unmanagedSourceDirectories := Seq((Test / scalaSource).value),
)

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true,
  )


mainClass := Some("quizz.web.WebApp")
dockerEntrypoint := Seq("/opt/docker/bin/quizz")
dockerExposedPorts := Seq(8080, 8080)
