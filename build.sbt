organization := "org.graylog2"

name := "graylog2-rest-client"

version := "0.21.0-beta4"

// disable using the Scala version in output paths and artifacts
crossPaths := false

resolvers in Global ++= Seq( Resolver.sonatypeRepo("snapshots"), Resolver.sonatypeRepo("releases") )

libraryDependencies ++= Seq(
  "org.graylog2" % "graylog2-rest-routes" % version.value changing() intransitive(),
  "com.google.inject" % "guice" % "3.0",
  "com.google.inject.extensions" % "guice-assistedinject" % "3.0",
  "com.google.code.gson" % "gson" % "2.2",
  "com.typesafe.play" %% "play-java" % "2.2.2",
  "com.typesafe.play" %% "play-cache" % "2.2.2",
  "org.apache.shiro" % "shiro-core" % "1.2.2",
  "javax.ws.rs" % "jsr311-api" % "0.11",
  "com.sun.jersey" % "jersey-bundle" % "1.17.1"
)

// No need to generate Javadoc here
sources in doc in Compile := List()
