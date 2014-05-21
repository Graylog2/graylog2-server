organization := "org.graylog2"

name := "graylog2-rest-client"

version := "0.21.0-SNAPSHOT"

// disable using the Scala version in output paths and artifacts
crossPaths := false

resolvers in Global ++= Seq( "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
                             "releases" at "http://oss.sonatype.org/content/repositories/releases",
                             "Typesafe Maven releases" at "http://repo.typesafe.com/typesafe/maven-releases",
                             "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/")

externalPom()
