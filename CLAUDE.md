# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Scala 3 project named "scala3_optimiser" using sbt as the build tool. The project uses Scala 3.3.6 and has the package prefix `com.guizmaii.scala.optimiser`.

## Build Commands

All sbt commands should be run with the `--client` flag for better performance:

- **Compile**: `sbt --client ctc` (shows compilation warnings)
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

This is a multi-module SBT project with the following structure:

### Root Directory
- `build.sbt` - Multi-module build configuration
- `project/plugins.sbt` - SBT plugins (assembly, JMH)
- `CLAUDE.md` - This file
- `primitive-option-implementation-plan.md` - Implementation plan

### Documentation
- `docs/` - Analysis and research documentation
  - `kyo-maybe-analysis.md` - Analysis of Kyo's Maybe implementation
  - `scala3-compiler-plugin-analysis.md` - Scala 3 compiler plugin research

### Modules

#### 1. Core Module (`core/`)
- **Purpose**: PrimitiveOption implementation
- **Path**: `core/src/main/scala/com/guizmaii/scala/optimiser/primitiveoption/`
- **Key Files**: `PrimitiveOption.scala` - Main opaque type implementation with zero-allocation encoding

#### 2. Plugin Module (`plugin/`)
- **Purpose**: Scala 3 compiler plugin for AST transformation
- **Path**: `plugin/src/main/scala/com/guizmaii/scala/optimiser/plugin/`
- **Resources**: `plugin/src/main/resources/scalac-plugin.xml` - Plugin configuration
- **Dependencies**: Depends on core module

#### 3. Tests Module (`tests/`)
- **Purpose**: Comprehensive test suite
- **Path**: `tests/src/test/scala/com/guizmaii/scala/optimiser/`
- **Dependencies**: Depends on core and plugin modules

#### 4. Benchmarks Module (`benchmarks/`)
- **Purpose**: JMH performance benchmarks
- **Path**: `benchmarks/src/main/scala/com/guizmaii/scala/optimiser/`
- **Dependencies**: Depends on core module, uses JMH plugin

All code should be placed under the package `com.guizmaii.scala.optimiser` or its subpackages.

## Scala Coding Rules

Never use the indentation-based syntax of Scala 3. Always use braces `{}` for code blocks.

Never claim that you're done adding some code if the code doesn't compile.

Never claim to have finished if the Scala compiler emits an error or a warning. Please fix these before claiming you're done.

All case classes should be final.

Never claim you're done implementing some code if you didn't write exhaustive test suite for the code you wrote.

When starting a pattern matching, always go to the next line first:

Don't:
```scala
def f(a: Option[Int]) = a match {
```

Do:
```scala
def f(a: Option[Int]) =
  a match {
```


## Test Structure and Naming Conventions

### Test File Organization
- Test files should be named `*Spec.scala` (e.g., `ExampleSpec.scala`)
- Tests should extend `ZIOSpecDefault`
- Place tests in the same package structure as the code being tested
- Use `object` instead of `class` for test specifications

### Test Structure Pattern
```scala
object MyClassSpec extends ZIOSpecDefault {
  
  // Define reusable test data at the top
  private val testData = ...
  
  // Group related tests into suites
  private val methodNameSpec = 
    suite("::methodName")(  // Use :: prefix for method names
      test("description of behavior") {
        // test implementation
      },
      test("another behavior") {
        // test implementation
      },
    )

  // Group related tests into suites
  private val functionNameSpec =
    suite(".functionName")(  // Use . prefix for function names
      test("description of behavior") {
        // test implementation
      },
      test("another behavior") {
        // test implementation
      },
    )
  
  // Main spec combines all test suites
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("MyClassSpec")(
      methodNameSpec,
      anotherMethodSpec,
      functionNameSpec,
      anotherFunctionSpec,
    )
}
```

### Test Naming Conventions

When you write your tests, group them by method or function:

- Suite names for methods: Use `::methodName` format (e.g., `suite("::targetNodeOutput")`)
- Suite names for functions: Use `.fromString`, `.apply` etc.
- Test descriptions: Use present tense, descriptive phrases
  - Good: `"returns empty Chunk for Failed state"`
  - Good: `"creates NodeFailure with error and code"`
  - Good: `"encodes Int error code as plain integer"`
  - Avoid: `"should return..."` or `"must return..."`

### Assertions
- Use `assertTrue` for simple boolean assertions
- Use property-based testing with `check` and generators for comprehensive testing
- Use custom assertions like `assertTrue(result.is(_.right.anything))` for Either types
- Group related assertions in a single `assertTrue` with multiple conditions

### Property-Based Testing
- Use ZIO Test's generators for property-based testing
- Apply test aspects like `@@ shrinks(0)` and `@@ samples(10)` to control test execution
- Use `check` with generators for exhaustive testing of properties

## Git Rules

Always `git add` all the files you add to the project immediately after creating them.

Never commit for for the user. The user will do the commits.

## Conversation Export Rules

Always keep the conversation export file (conversation_export.txt) updated. Once you're done doing something, you need to update this file with our latest conversation so that it's easy to restore our conversation in the future.