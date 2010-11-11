import java.util.NoSuchElementException
import org.graylog2.Tools
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

@RunWith(classOf[JUnitRunner])
class GraylogServerSpec extends Spec with ShouldMatchers {

  describe("Graylog Server") {

    describe("Tools") {

      describe("getting the PID") {

        it("should return something") {
          Tools.getPID() should not be ('empty)
        }

        it("should return a process id larger than 0") {
          Integer.parseInt(Tools.getPID()) should be > 0
        }
      }

      describe("converting syslog levels to human readables") {

        it("should explain syslog levels") {
          Tools.syslogLevelToReadable(0) should be ("Emergency")
        }

        it("should complain when asked for invalid log levels") {
          Tools.syslogLevelToReadable(12351243) should be ("Invalid")
        }
      }
    }
  }
}
