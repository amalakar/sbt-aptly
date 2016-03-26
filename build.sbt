name := "sbt-aptly"
organization := "com.github.amalakar"
version := "1.0.0"

scalaVersion := "2.10.4"
sbtPlugin := true

libraryDependencies ++= Seq(
  "org.apache.httpcomponents" % "httpclient" % "4.5.2",
  "org.apache.httpcomponents" % "httpmime" % "4.5.2",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

publishMavenStyle := false
