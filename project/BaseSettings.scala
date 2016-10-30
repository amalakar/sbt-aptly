import sbt.Keys._
import sbt._

object BaseSettings {

  lazy val defaultSettings =
    Seq(
      organization := "org.kaloz.sbt.plugin",
      name := "sbt-aptly",
      description := "SBT Aptly plugin",
      scalaVersion := "2.10.4",
      licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
      homepage := Some(url("http://kaloz.org")),
      crossPaths := false,
      sbtPlugin := true,
      scalacOptions := Seq(
        "-encoding", "utf8",
        "-feature",
        "-unchecked",
        "-deprecation",
        "-language:postfixOps",
        "-language:implicitConversions"
      ),
      javacOptions := Seq(
        "-Xlint:unchecked",
        "-Xlint:deprecation"
      ),
      shellPrompt := { s => "[" + scala.Console.BLUE + Project.extract(s).currentProject.id + scala.Console.RESET + "] $ " }
    ) ++
      Resolvers.defaultSettings
}
