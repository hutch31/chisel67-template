ThisBuild / scalaVersion := "2.13.13"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"

val chiselVersion = "6.7.0"
val chiseltestVersion = "6.0.0"

lazy val root = (project in file("."))
  .settings(
    name := "ChiselTemplate67",
    libraryDependencies ++= Seq(
      "org.chipsalliance" %% "chisel" % chiselVersion,
      "edu.berkeley.cs" %% "chiseltest" % "6.0.0" % "test",
      "org.scalatest" %% "scalatest" % "3.2.14" % "test"
    ),

    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
      "-Ymacro-annotations"
    ),
    addCompilerPlugin("org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full),
  )

