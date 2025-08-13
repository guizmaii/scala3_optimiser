ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.6"

lazy val root = (project in file("."))
  .settings(
    name := "scala3_optimiser",
    idePackagePrefix := Some("com.guizmaii.scala.optimiser")
  )
