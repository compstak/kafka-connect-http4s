val scala212 = "2.12.10"
val scala213 = "2.13.1"
val supportedScalaVersions = List(scala212, scala213)

enablePlugins(DockerComposePlugin)

inThisBuild(
  List(
    scalaVersion := scala213,
    organization := "com.compstak",
    homepage := Some(url("https://github.com/compstak/circe-geojson")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "LukaJCB",
        "Luka Jacobowitz",
        "luka.jacobowitz@gmail.com",
        url("https://github.com/LukaJCB")
      ),
      Developer(
        "pedrofurla",
        "Pedro Furlanetto",
        "pedrofurla@gmail.com",
        url("https://github.com/pedrofurla")
      ),
      Developer(
        "goedelsoup",
        "Cory Parent",
        "goedelsoup@gmail.com",
        url("https://github.com/goedelsoup")
      )
    )
  )
)

scalacOptions in ThisBuild ++= Seq(
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

addCommandAlias("fmtAll", ";scalafmt; test:scalafmt; scalafmtSbt; it:scalafmt")
addCommandAlias("fmtCheck", ";scalafmtCheck; test:scalafmtCheck; scalafmtSbtCheck; it:scalafmtCheck")
addCommandAlias("validate", ";fmtCheck; test; it:compile")

val CirceVersion = "0.13.0"
val Fs2Version = "2.2.2"
val Http4sVersion = "0.21.15"
val ScalatestVersion = "3.1.0"

lazy val commonSettings = Seq(
  scalafmtOnCompile := true,
  crossScalaVersions := supportedScalaVersions,
  addCompilerPlugin(("org.typelevel" %% "kind-projector" % "0.11.0").cross(CrossVersion.full)),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
)

lazy val noPublishSettings = Seq(
  skip in publish := true
)

lazy val client = (project in file("client"))
  .settings(commonSettings: _*)
  .settings(
    name := "kafka-connect-http4s",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-client" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion
    )
  )

lazy val migrate = (project in file("migrate"))
  .configs(IntegrationTest)
  .settings(commonSettings: _*)
  .settings(
    name := "kafka-connect-migrate",
    Defaults.itSettings,
    inConfig(IntegrationTest)(ScalafmtPlugin.scalafmtConfigSettings),
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-io" % Fs2Version,
      "io.circe" %% "circe-parser" % CirceVersion,
      "io.circe" %% "circe-literal" % CirceVersion % IntegrationTest,
      "org.http4s" %% "http4s-async-http-client" % Http4sVersion % IntegrationTest,
      "org.scalatest" %% "scalatest" % ScalatestVersion % IntegrationTest
    )
  )
  .dependsOn(client)

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    name := "kafka-connect",
    publish := {}
  )
  .aggregate(client, migrate)
