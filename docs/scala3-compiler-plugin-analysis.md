# Scala 3 Compiler Plugin Analysis

## Overview

Scala 3 introduces a new compiler plugin system that is incompatible with Scala 2 plugins. There are two types of plugins:

1. **StandardPlugin**: For common use cases (99% of plugins)
2. **ResearchPlugin**: For experimental compiler pipeline modifications

## Key Differences from Scala 2

- StandardPlugin cannot modify type information at compile time (unlike Scala 2's AnalyzerPlugin)
- New API structure with PluginPhase extending MiniPhase
- Different phase naming and ordering system

## Plugin Structure

### Basic Plugin Class
```scala
class MyPlugin extends StandardPlugin:
  val name: String = "myPlugin"
  override val description: String = "My plugin description"
  
  override def initialize(options: List[String])(using Context): List[PluginPhase] =
    (new MyPluginPhase) :: Nil
```

### Plugin Phase Implementation
```scala
class MyPluginPhase extends PluginPhase:
  import tpd.*
  
  val phaseName = "myPhase"
  override val runsAfter = Set("typer")
  override val runsBefore = Set("firstTransform")
  
  // Transform methods for different AST nodes
  override def transformApply(tree: Apply)(using ctx: Context): Tree = tree
  override def transformSelect(tree: Select)(using ctx: Context): Tree = tree
  override def transformIdent(tree: Ident)(using ctx: Context): Tree = tree
  override def transformTypeTree(tree: TypeTree)(using ctx: Context): Tree = tree
  override def transformValDef(tree: ValDef)(using ctx: Context): Tree = tree
  override def transformDefDef(tree: DefDef)(using ctx: Context): Tree = tree
```

## Compiler Phases

### Phase Groups
1. **frontendPhases**: parser, typer, posttyper
2. **picklerPhases**: pickler, inlining, staging
3. **transformPhases**: 
   - High-level: firstTransform → erasure
   - Low-level: ElimErasedValueType → CollectSuperCalls
4. **backendPhases**: Generate classfiles/SJSIR

### Key Phase Names for Plugin Constraints
- `parser`: Parsing phase
- `typer`: Type checking phase
- `posttyper`: Post type checking cleanup
- `pickler`: TASTy serialization
- `firstTransform`: First transformation phase
- `patternMatcher`: Pattern matching expansion
- `erasure`: Type erasure phase

### Optimal Insertion Points for Option Transformation
- **After typer**: Types are fully resolved
- **Before firstTransform**: Early in transformation pipeline
- **Before erasure**: Still have full type information

## Transform Methods Available

PluginPhase provides numerous transform methods:
- `transformApply`: Function applications
- `transformSelect`: Field/method selections
- `transformIdent`: Identifier references
- `transformTypeTree`: Type trees
- `transformValDef`: Value definitions
- `transformDefDef`: Method definitions
- `transformBlock`: Code blocks
- `transformIf`: If expressions
- `transformMatch`: Match expressions
- `transformTry`: Try-catch blocks
- `transformNew`: New instance creation
- `transformTypeDef`: Type definitions
- `transformTemplate`: Class/trait templates

## Plugin Configuration

### SBT Setup (1.1.5+)
```scala
// In build.sbt
scalacOptions += "-Xplugin:path/to/plugin.jar"
scalacOptions += "-P:pluginName:option1:option2"
```

### Plugin JAR Requirements
- Must include `scalac-plugin.xml` in resources:
```xml
<plugin>
  <name>myPlugin</name>
  <classname>com.example.MyPlugin</classname>
</plugin>
```

### Option Handling
```scala
override def initialize(options: List[String])(using Context): List[PluginPhase] =
  // Parse options like "verbose", "aggressive", etc.
  val verbose = options.contains("verbose")
  (new MyPluginPhase(verbose)) :: Nil
```

## Example: Divide by Zero Checker
```scala
class DivideZeroPhase extends PluginPhase:
  import tpd.*
  
  val phaseName = "divideZero"
  override val runsAfter = Set(Pickler.name)
  override val runsBefore = Set(Staging.name)
  
  override def transformApply(tree: Apply)(using ctx: Context): Tree =
    tree match
      case Apply(Select(rcvr, nme.DIV), List(Literal(Constant(0)))) 
        if rcvr.tpe <:< defn.IntType =>
          report.error("divide by zero", tree.srcPos)
          tree
      case _ => tree
```

## Critical Implementation Notes for PrimitiveOption Plugin

1. **Phase Placement**: Insert after `typer` but before `firstTransform`
2. **Type Transformation**: Focus on transforming:
   - Type references (`TypeTree`)
   - Constructor calls (`Apply`, `New`)
   - Pattern matching (`Match`)
   - Method calls on Option (`Select`)
3. **Import Context**: Always `import tpd.*` for typed trees
4. **Error Handling**: Use `report.error()` for compilation errors
5. **Type Checking**: Use `<:<` for subtype checks
6. **Pattern Matching**: Transform both extractors and type patterns

## Resources

- [Official Scala 3 Plugin Documentation](https://docs.scala-lang.org/scala3/reference/changed-features/compiler-plugins.html)
- [EPFL Dotty Plugin Documentation](https://dotty.epfl.ch/docs/reference/changed-features/compiler-plugins.html)
- [Example Plugin Repositories](https://github.com/liufengyun/scala3-plugin-example)