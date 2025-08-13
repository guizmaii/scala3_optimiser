# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Scala 3 project named "scala3_optimiser" using sbt as the build tool. The project uses Scala 3.3.6 and has the package prefix `com.guizmaii.scala.optimiser`.

## Build Commands

All sbt commands should be run with the `--client` flag for better performance:

- **Compile**: `sbt --client compile`
- **Run tests**: `sbt --client test`
- **Run a specific test**: `sbt --client "testOnly *TestClassName"`
- **Run a specific test method**: `sbt --client "testOnly *TestClassName -- -z \"test name\"`
- **Clean**: `sbt --client clean`
- **Format code**: `sbt --client fmt` (if scalafmt is configured)
- **Run the application**: `sbt --client run`
- **Interactive console**: `sbt --client console`
- **Package**: `sbt --client package`
- **Create fat JAR**: `sbt --client assembly` (if sbt-assembly plugin is added)

## Project Structure

The project follows standard sbt conventions:
- Source code: `src/main/scala/`
- Test code: `src/test/scala/`
- Build configuration: `build.sbt`
- Project plugins: `project/plugins.sbt`

All code should be placed under the package `com.guizmaii.scala.optimiser` or its subpackages.