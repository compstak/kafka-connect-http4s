resolvers += Resolver.jcenterRepo

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.5.2")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.11")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.0.2")
addSbtPlugin("com.frugalmechanic" % "fm-sbt-s3-resolver" % "0.18.0")
addSbtPlugin("org.lyranthe.sbt" % "partial-unification" % "1.1.2")
