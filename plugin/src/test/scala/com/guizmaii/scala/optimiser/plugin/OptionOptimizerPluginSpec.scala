package com.guizmaii.scala.optimiser.plugin

import zio.test.*
import zio.Scope

object OptionOptimizerPluginSpec extends ZIOSpecDefault {

  private val pluginBasicsSpec =
    suite("::plugin basics")(
      test("has correct name") {
        val plugin = new OptionOptimizerPlugin
        assertTrue(plugin.name == "option-optimizer")
      },
      test("has correct description") {
        val plugin              = new OptionOptimizerPlugin
        val expectedDescription = "Scala 3 compiler plugin that replaces Option with Kyo's Maybe for zero-allocation performance"
        assertTrue(plugin.description == expectedDescription)
      }
    )

  private val initEmptyOptionsSpec =
    suite("::init with empty options")(
      test("returns single phase with default config") {
        val plugin = new OptionOptimizerPlugin
        val phases = plugin.init(List.empty)
        assertTrue(
          phases.length == 1,
          phases.head.isInstanceOf[OptionToMaybePhase]
        )
      }
    )

  private val initWithOptionsSpec =
    suite("::init with options")(
      test("creates phase with verbose config") {
        val plugin = new OptionOptimizerPlugin
        val phases = plugin.init(List("verbose"))
        assertTrue(
          phases.length == 1,
          phases.head.isInstanceOf[OptionToMaybePhase]
        )
        // Note: We can't easily test the internal config of the phase
        // without exposing it, which would break encapsulation
      },
      test("creates phase with multiple options") {
        val plugin  = new OptionOptimizerPlugin
        val options = List("verbose", "aggressive", "exclude:com.example")
        val phases  = plugin.init(options)
        assertTrue(
          phases.length == 1,
          phases.head.isInstanceOf[OptionToMaybePhase]
        )
      },
      test("creates phase with unknown options") {
        val plugin  = new OptionOptimizerPlugin
        val options = List("unknown", "invalid-option")
        val phases  = plugin.init(options)
        assertTrue(
          phases.length == 1,
          phases.head.isInstanceOf[OptionToMaybePhase]
        )
      }
    )

  private val initPhasePropertiesSpec =
    suite("::init phase properties")(
      test("created phase has correct name") {
        val plugin = new OptionOptimizerPlugin
        val phases = plugin.init(List.empty)
        val phase  = phases.head.asInstanceOf[OptionToMaybePhase]
        assertTrue(phase.phaseName == "option-to-maybe")
      },
      test("created phase has correct ordering constraints") {
        val plugin = new OptionOptimizerPlugin
        val phases = plugin.init(List.empty)
        val phase  = phases.head.asInstanceOf[OptionToMaybePhase]
        assertTrue(
          phase.runsAfter.contains("pickler"),
          phase.runsBefore.contains("firstTransform")
        )
      }
    )

  private val initConsistencySpec =
    suite("::init consistency")(
      test("multiple calls with same options produce equivalent phases") {
        val plugin  = new OptionOptimizerPlugin
        val options = List("verbose", "exclude:com.test")

        val phases1 = plugin.init(options)
        val phases2 = plugin.init(options)

        assertTrue(
          phases1.length == phases2.length,
          phases1.head.getClass == phases2.head.getClass,
          phases1.head.phaseName == phases2.head.phaseName
        )
      },
      test("different option orders produce equivalent phases") {
        val plugin   = new OptionOptimizerPlugin
        val options1 = List("verbose", "aggressive")
        val options2 = List("aggressive", "verbose")

        val phases1 = plugin.init(options1)
        val phases2 = plugin.init(options2)

        assertTrue(
          phases1.length == phases2.length,
          phases1.head.getClass == phases2.head.getClass,
          phases1.head.phaseName == phases2.head.phaseName
        )
      }
    )

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("OptionOptimizerPluginSpec")(
      pluginBasicsSpec,
      initEmptyOptionsSpec,
      initWithOptionsSpec,
      initPhasePropertiesSpec,
      initConsistencySpec
    )
}
