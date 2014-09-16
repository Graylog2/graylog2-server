import sbt._
import Keys._
import play.Project._
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}
import com.typesafe.sbt.SbtNativePackager.Universal
import com.typesafe.sbt.packager.universal.Keys.packageZipTarball

object ApplicationBuild extends Build {
  val appName         = "graylog2-web-interface"
  val appVersion      = "0.21.0-rc.1"
  val appDependencies = Seq(
    cache,
    javaCore,
    javaEbean,
    "com.google.code.gson" % "gson" % "2.2",
    "com.google.guava" % "guava" % "14.0",
    "com.ning" % "async-http-client" % "1.7.17",
    "org.apache.shiro" % "shiro-core" % "1.2.2",
    "com.google.inject" % "guice" % "3.0",
    "com.google.inject.extensions" % "guice-assistedinject" % "3.0",
    "javax.inject" % "javax.inject" % "1",
    "org.graylog2" % "play2-graylog2_2.10" % "1.0",

    // TODO this is stupid, just to get that UriBuilder...
    "javax.ws.rs" % "jsr311-api" % "0.11",
    "com.sun.jersey" % "jersey-server" % "1.17.1",
    "com.sun.jersey" % "jersey-grizzly2" % "1.17.1",
    "com.sun.jersey" % "jersey-bundle" % "1.17.1",

    "org.mockito" % "mockito-all" % "1.9.5" % "test",
    "org.elasticsearch" % "elasticsearch" % "0.90.5" % "test",
    "org.fluentlenium" % "fluentlenium-core" % "0.9.0" % "test",
    "org.fluentlenium" % "fluentlenium-festassert" % "0.9.0" % "test",
    "org.codehaus.jackson" % "jackson-core-asl" % "1.9.12" % "test"
  )
  val repositories = Seq(
    Resolver.url("Graylog2 Play Repository", url("http://graylog2.github.io/play2-graylog2/releases/"))(Resolver.ivyStylePatterns),
    Resolver.url("Graylog2 Play Snapshot Repository", url("http://graylog2.github.io/play2-graylog2/snapshots/"))(Resolver.ivyStylePatterns),
    Resolver.sonatypeRepo("releases"),
    ("Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
  )

  // Submodule
  lazy val restClient = Project("graylog2-rest-client", file("modules/graylog2-rest-client"))

  // Helper
  val isSnapshot: Boolean = appVersion.endsWith("SNAPSHOT")
  val timestamp: String = {
    val df = new SimpleDateFormat("yyyyMMddHHmmss")
    df.setTimeZone(TimeZone.getTimeZone("UTC"))
    df.format(new Date())
  }

  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers ++= repositories,
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
    mappings in Universal in packageZipTarball += file("misc/graylog2-web-interface.conf.example") -> "conf/graylog2-web-interface.conf",
    name in Universal := {
      val originalName = (name in Universal).value
      if (isSnapshot) {
        s"${appName}-${appVersion}-${timestamp}"
      } else {
        originalName
      }
    }
  ).dependsOn(restClient)
}
