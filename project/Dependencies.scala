import sbt.Keys._
import sbt._

object Version {

  val httpcomponents      = "4.5.2"
  val scalaTest           = "2.2.6"
}

object Library {
  val httpClient = "org.apache.httpcomponents"     % "httpclient"         % Version.httpcomponents
  val httpMime   = "org.apache.httpcomponents"     % "httpmime"           % Version.httpcomponents
  val scalaTest  = "org.scalatest"                %% "scalatest"          % Version.scalaTest
}

object Dependencies {

  import Library._

  val plugin = deps(
    httpClient,
    httpMime,
    scalaTest     	  % "test"
  )

  private def deps(modules: ModuleID*): Seq[Setting[_]] = Seq(libraryDependencies ++= modules)
}

