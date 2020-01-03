val scala212 = "2.12.10"
val scala213 = "2.13.1"
val supportedScalaVersions = List(scala212, scala213)

ThisBuild / organization := "compstak"
ThisBuild / scalaVersion := scala212

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Ywarn-value-discard",
  "-Ywarn-dead-code",
  "-Xlint:infer-any",
  "-Xlint:nullary-override",
  "-Xlint:nullary-unit",
  "-Xfatal-warnings"
)

addCommandAlias("fmtAll", ";scalafmt; test:scalafmt; scalafmtSbt")
addCommandAlias("fmtCheck", ";scalafmtCheck; test:scalafmtCheck; scalafmtSbtCheck")

val http4sVersion = "0.21.0-M6"

lazy val commonSettings = Seq(
  addCompilerPlugin(("org.typelevel" % "kind-projector" % "0.11.0").cross(CrossVersion.full)),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
  publishTo := {
    val prefix = if (isSnapshot.value) "snapshots" else "releases"
    Some("CompStak".at("s3://compstak-maven.s3-us-east-1.amazonaws.com/" + prefix))
  }
)

lazy val publishSettings = Seq(
  crossScalaVersions := supportedScalaVersions,
  releaseCrossBuild := true,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  }
)

lazy val client = (project in file("client"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "kafka-connect-http4s",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-client" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion
    )
  )

lazy val migrate = (project in file("migrate"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "kafka-connect-migrate",
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-io" % "2.1.0",
      "io.circe" %% "circe-parser" % "0.12.3"
    )
  )
  .dependsOn(client)

lazy val root = (project in file("."))
  .settings(sharedSettings)
  .settings(
    name := "kafka-connect",
    publish := {}
  )
  .aggregate(client, migrate)
