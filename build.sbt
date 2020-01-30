val scala212 = "2.12.10"
val scala213 = "2.13.1"
val supportedScalaVersions = List(scala212, scala213)

ThisBuild / organization := "compstak"
ThisBuild / scalaVersion := scala212

enablePlugins(DockerComposePlugin)

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

credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "nexus.compstak.com",
  sys.env.get("NEXUS_USERNAME").getOrElse(""),
  sys.env.get("NEXUS_PASSWORD").getOrElse("")
)

val http4sVersion = "0.21.0-RC1"

lazy val commonSettings = Seq(
  addCompilerPlugin(("org.typelevel" % "kind-projector" % "0.11.0").cross(CrossVersion.full)),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
  publishTo := {
    val suffix = if (isSnapshot.value) "snapshots" else "releases"
    Some("CompStak".at(s"https://nexus.compstak.com/repository/maven-$suffix"))
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
  .configs(IntegrationTest)
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "kafka-connect-migrate",
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-io" % "2.2.1",
      "io.circe" %% "circe-parser" % "0.12.3",
      "org.http4s" %% "http4s-async-http-client" % http4sVersion % IntegrationTest,
      "org.scalatest" %% "scalatest" % "3.1.0" % IntegrationTest
    )
  )
  .dependsOn(client)

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(
    name := "kafka-connect",
    publish := {}
  )
  .aggregate(client, migrate)
