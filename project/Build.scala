import sbt._
import Keys._
import play.Project._
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}
import com.typesafe.sbt.SbtNativePackager.Universal
import com.typesafe.sbt.packager.universal.Keys.packageZipTarball

object ApplicationBuild extends Build {

  val appName         = "graylog2-web-interface"
  val appVersion      = "0.21.0-SNAPSHOT"


  // use this to potentially add a timestamp to the built package name
  def packageSnapshot = Command.command("package-snapshot") { state =>
    val extracted = Project extract state

    val configFile: File = new File("conf/graylog2-web-interface.conf")
    val exampleConfigFile: File = new File("misc/graylog2-web-interface.conf.example")

    // this will overwrite the config file with the sample one
    IO.copyFile(exampleConfigFile, configFile, preserveLastModified = true)

    val task: (State, File) = extracted.runTask(packageZipTarball in Universal, state)

    val snapshot = extracted.getOpt(isSnapshot).get // getting current version
    if (snapshot) {
      val dateFormat = { val df = new SimpleDateFormat("yyyyMMddhhmmss");
        df.setTimeZone(TimeZone.getTimeZone("UTC")); df
      }
      val timestamp = dateFormat.format(new Date())
      // fuck it sbt, really.
      // changing the state to use a different package name is not possible, because the artifact name has already been
      // constructed at the time this runs. instead of doing it lazily the native packager does it eagerly, so we can't
      // change it anymore.
      val packageDir: File = new File(task._2.getParentFile, "package")
      packageDir.mkdir()
      task._2.renameTo(new File(packageDir, task._2.getName.replace(".tgz", "-" + timestamp + ".tgz")))
    }
    task._1
  }

  val appDependencies = Seq(
    cache,
    javaCore,
    javaJdbc,
    javaEbean,

    "com.google.code.gson" % "gson" % "2.2",
    "com.google.guava" % "guava" % "14.0",
    "com.ning" % "async-http-client" % "1.7.17",
    "org.apache.shiro" % "shiro-core" % "1.2.2",
    "com.google.inject" % "guice" % "3.0",
    "com.google.inject.extensions" % "guice-assistedinject" % "3.0",
    "javax.inject" % "javax.inject" % "1",

    "org.graylog2" % "play2-graylog2_2.10" % "1.0",
//    "org.graylog2" % "graylog2-rest-client" % "0.21.0-SNAPSHOT" changing(),

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

  lazy val restClient = Project("graylog2-rest-client", file("modules/graylog2-rest-client"))

  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += Resolver.url("Graylog2 Play Repository", url("http://graylog2.github.io/play2-graylog2/releases/"))(Resolver.ivyStylePatterns),
    resolvers += Resolver.url("Graylog2 Play Snapshot Repository", url("http://graylog2.github.io/play2-graylog2/snapshots/"))(Resolver.ivyStylePatterns),
    resolvers += Resolver.url("Typesafe Maven Releases", url("http://repo.typesafe.com/typesafe/maven-releases"))(Resolver.mavenStylePatterns),
    resolvers += ("Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"),
    commands ++= Seq(packageSnapshot),
    resourceGenerators in Compile <+= resourceManaged in Compile map { dir =>
      val propsFile = new File(dir, "git.properties")
      IO.write(propsFile, "git.sha1=%s\n".format("git rev-parse HEAD" !!))
      Seq(propsFile)
    }

  ).dependsOn(restClient)
}
