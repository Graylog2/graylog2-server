import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "graylog2-web-interface"
  val appVersion      = "0.20.1"

  val appDependencies = Seq(
    cache,
    javaCore,
    javaJdbc,
    javaEbean,

    "com.google.code.gson" % "gson" % "2.2",
    "com.google.guava" % "guava" % "14.0.1",
    "com.ning" % "async-http-client" % "1.7.17",
    "org.apache.shiro" % "shiro-core" % "1.2.2",
    "com.google.inject" % "guice" % "3.0",
    "com.google.inject.extensions" % "guice-assistedinject" % "3.0",
    "javax.inject" % "javax.inject" % "1",

    "org.graylog2" % "play2-graylog2_2.10" % "1.0",

    "org.elasticsearch" % "elasticsearch" % "0.90.5" % "test",

    "org.fluentlenium" % "fluentlenium-core" % "0.9.0" % "test",
    "org.fluentlenium" % "fluentlenium-festassert" % "0.9.0" % "test",
    "org.mockito" % "mockito-all" % "1.9.5" % "test",

    // TODO this is stupid, just to get that UriBuilder...
    "javax.ws.rs" % "jsr311-api" % "0.11",
    "com.sun.jersey" % "jersey-server" % "1.17.1",
    "com.sun.jersey" % "jersey-grizzly2" % "1.17.1",
    "com.sun.jersey" % "jersey-bundle" % "1.17.1",

    "org.codehaus.jackson" % "jackson-core-asl" % "1.9.12" % "test"
)

  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += Resolver.url("Graylog2 Play Repository", url("http://graylog2.github.io/play2-graylog2/releases/"))(Resolver.ivyStylePatterns),
    resolvers += Resolver.url("Graylog2 Play Snapshot Repository", url("http://graylog2.github.io/play2-graylog2/snapshots/"))(Resolver.ivyStylePatterns)
  )

}
