import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "graylog2WebInterface"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    javaCore,
    javaJdbc,
    javaEbean,

    "com.google.code.gson" % "gson" % "2.2",
    "com.ning" % "async-http-client" % "1.7.17",

    "org.apache.shiro" % "shiro-core" % "1.2.2",

    "org.mockito" % "mockito-all" % "1.9.5" % "test",
    "org.seleniumhq.selenium" % "selenium-java" % "2.33.0" % "test",
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
