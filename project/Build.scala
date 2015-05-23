import sbt._
import Keys._
import play.Play.autoImport._
import PlayKeys._
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}
import com.typesafe.sbt.SbtNativePackager.Universal
import com.typesafe.sbt.packager.universal.Keys.packageZipTarball
import com.typesafe.sbt.web.SbtWeb
import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.less.Import.LessKeys

object ApplicationBuild extends Build {
  val appName         = "graylog-web-interface"
  val appVersion      = "1.1.0-beta.3-SNAPSHOT"
  val appDependencies = Seq(
    cache,
    javaCore,
    "com.google.guava" % "guava" % "18.0",
    "com.ning" % "async-http-client" % "1.8.14",
    "org.apache.shiro" % "shiro-core" % "1.2.2",
    "com.google.inject" % "guice" % "4.0",
    "com.google.inject.extensions" % "guice-assistedinject" % "4.0",
    "javax.inject" % "javax.inject" % "1",
    "org.graylog2" % "play2-graylog2_2.10" % "1.2.1",
    "org.graylog2" % "graylog2-rest-client" % appVersion,

    "com.github.fdimuccio" %% "play2-sockjs" % "0.3.1",

    "junit" % "junit" % "4.12" % "test",
    "org.mockito" % "mockito-all" % "1.9.5" % "test",
    "org.assertj" % "assertj-core" % "2.0.0" % "test"
  )
  val repositories = Seq(
    Resolver.mavenLocal,
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots"),
    Resolver.typesafeRepo("releases")
  )

  // Helper
  val isSnapshot: Boolean = appVersion.endsWith("SNAPSHOT")
  val timestamp: String = {
    val df = new SimpleDateFormat("yyyyMMddHHmmss")
    df.setTimeZone(TimeZone.getTimeZone("UTC"))
    df.format(new Date())
  }

  val main = Project(appName, file(".")).enablePlugins(play.PlayJava).enablePlugins(SbtWeb).settings(
    scalaVersion := "2.10.4",
    version := appVersion,
    libraryDependencies ++= appDependencies,
    resolvers ++= repositories,
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
    resourceGenerators in Compile <+= resourceManaged in Compile map { dir =>
      val propsFile = new File(dir, "git.properties")
      val currentGitSha = "git.sha1=%s\n".format("git rev-parse HEAD" !!)
      var writtenGitSha = ""

      if (propsFile.exists()) {
        writtenGitSha = IO.read(propsFile)
      }

      // Only write the git.properties file if the content will actually change.
      // This prevents an issue we have seen where asset delivery in dev mode
      // takes really long because the git.properties file was overwritten
      // multiple times and caused file-changed events. (as far as we understood)
      if (currentGitSha != writtenGitSha) {
        IO.write(propsFile, currentGitSha)
      }
      Seq(propsFile)
    },
    sources in doc in Compile := List(),
    includeFilter in (Assets, LessKeys.less) := "*.less",
    mappings in Universal in packageZipTarball += file("misc/graylog-web-interface.conf.example") -> "conf/graylog-web-interface.conf",
    name in Universal := {
      val originalName = (name in Universal).value
      if (isSnapshot) {
        s"${appName}-${appVersion}-${timestamp}"
      } else {
        originalName
      }
    }
  )
}
