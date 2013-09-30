import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "graylog2-web-interface"
  val appVersion      = "0.20.0-preview.1"

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
    "org.fluentlenium" % "fluentlenium-core" % "0.9.0" % "test",
    "org.fluentlenium" % "fluentlenium-festassert" % "0.9.0" % "test",
    "org.mockito" % "mockito-all" % "1.9.5" % "test",
    "javax.ws.rs" % "jsr311-api" % "0.11" % "test",
    "com.sun.jersey" % "jersey-grizzly2" % "1.17.1" % "test",
    "com.sun.jersey" % "jersey-bundle" % "1.17.1" % "test",
    "com.sun.jersey" % "jersey-server" % "1.17.1" % "test",
    "org.codehaus.jackson" % "jackson-core-asl" % "1.9.12" % "test"
)

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
