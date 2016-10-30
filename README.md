This is a forked, updated and released version of [amalakar's projects](https://github.com/amalakar/sbt-aptly).

SBT plugin for aptly
====================

A plugin to upload debian packages to [aptly](https://www.aptly.info/) debian repo. It works in conjunction with [sbt-native-packager](https://github.com/sbt/sbt-native-packager)

Usage
------
You need to add the following in your `build.sbt`. To upload to the aptly debian repository you would want to do `sbt assembly debian:packageBin aptly-publish`

    enablePlugins(AptlyPlugin)

    aptlyUrl := "http://aptly.example.net:8080/api"
    aptlyPrefix := "analytics_precise"
    aptlyName := "prod"
    aptlyDistribution := "precise"
    aptlyPublishForceOverwrite := true
    aptlyDebianPackage := baseDirectory.value / s"target/${(name in Debian).value}_${version.value}_${(packageArchitecture in Debian).value}.deb"

