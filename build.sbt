import sbt.Keys.scalacOptions

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / version           := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion      := "3.7.1"
ThisBuild / organization      := "com.guizmaii.scala.optimiser"
ThisBuild / scalafmtCheck     := true
ThisBuild / scalafmtSbtCheck  := true
ThisBuild / scalafmtOnCompile := !insideCI.value
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision // use Scalafix compatible version
ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-language:higherKinds"
)

// compilation commands
addCommandAlias("tc", "Test/compile")
addCommandAlias("ctc", "clean; tc")
addCommandAlias("rctc", "reload; ctc")
addCommandAlias("fmt", "scalafmt; scalafmtSbt")

lazy val commonSettings = Seq(
  idePackagePrefix := Some("com.guizmaii.scala.optimiser"),
  scalacOptions ++= Seq(
    "-no-indent",
    "-language:noAutoTupling"
  ),
  scalacOptions --= (if (insideCI.value) Nil else Seq("-Xfatal-warnings")),
)

lazy val root = (project in file("."))
  .settings(
    name         := "scala3_optimiser",
    publish      := {},
    publishLocal := {}
  )
  .aggregate(plugin, tests, benchmarks)


lazy val plugin = (project in file("plugin"))
  .settings(commonSettings: _*)
  .settings(
    name                             := "scala3-option-optimizer-plugin",
    libraryDependencies ++= Seq(
      "io.getkyo"      %% "kyo-data"        % "1.0-RC1"          % Provided,
      "org.scala-lang" %% "scala3-compiler" % scalaVersion.value % Provided,
      "dev.zio"        %% "zio-test"        % "2.1.20"           % Test,
      "dev.zio"        %% "zio-test-sbt"    % "2.1.20"           % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    // Plugin JAR configuration
    assembly / assemblyMergeStrategy := {
      case "scalac-plugin.xml" => MergeStrategy.first
      case x                   => (assembly / assemblyMergeStrategy).value(x)
    },
    assembly / assemblyJarName       := "scala3-option-optimizer-plugin.jar"
  )

lazy val tests = (project in file("tests"))
  .settings(commonSettings: _*)
  .settings(
    name         := "scala3-option-optimizer-tests",
    libraryDependencies ++= Seq(
      "io.getkyo" %% "kyo-data"     % "1.0-RC1",
      "dev.zio"   %% "zio-test"     % "2.1.20" % Test,
      "dev.zio"   %% "zio-test-sbt" % "2.1.20" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    publish      := {},
    publishLocal := {}
  )
  .dependsOn(plugin)

lazy val benchmarks = (project in file("benchmarks"))
  .enablePlugins(JmhPlugin)
  .settings(commonSettings: _*)
  .settings(
    name         := "scala3-option-optimizer-benchmarks",
    libraryDependencies ++= Seq(
      "io.getkyo"       %% "kyo-data"                % "1.0-RC1",
      "org.openjdk.jmh" % "jmh-core"                 % "1.37",
      "org.openjdk.jmh" % "jmh-generator-annprocess" % "1.37"
    ),
    publish      := {},
    publishLocal := {}
  )
