package sbtaptly

import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import sbt.{Level, Logger}

class AptlyPluginSpec extends FunSpec with Matchers with BeforeAndAfter {
  describe("AptlyPlugin") {

    class TestLogger extends Logger {
      override def trace(t: => Throwable): Unit = log(Level.Debug, t.getMessage)
      override def log(level: Level.Value, message: => String): Unit = println(message)
      override def success(message: => String): Unit = log(Level.Info, message)
    }
    AptlyPlugin.logger = new TestLogger()

    it("should upload debian package") {
      // TODO this is more like an integration test, but still is handy for debugging
      val debFile = new java.io.File("/Users/arup/repos/analytics-flume-plugins/target/analytics-flume-plugins_1.0.2_all.deb")
      AptlyPlugin.publishAptlyPackage("http://localhost:8080/api", "analytics", "precise", debFile, new TestLogger())
    }
  }
}
