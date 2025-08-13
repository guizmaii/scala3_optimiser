package com.guizmaii.scala.optimiser.plugin

import zio.test.*
import zio.Scope

object OptionToMaybePhaseSpec extends ZIOSpecDefault {

  private val phaseBasicsSpec =
    suite("::phase basics")(
      test("has correct phase name") {
        val config = PluginConfig()
        val phase  = new OptionToMaybePhase(config)
        assertTrue(phase.phaseName == "option-to-maybe")
      },
      test("has correct phase ordering - runs after pickler") {
        val config = PluginConfig()
        val phase  = new OptionToMaybePhase(config)
        assertTrue(phase.runsAfter.contains("pickler"))
      },
      test("has correct phase ordering - runs before firstTransform") {
        val config = PluginConfig()
        val phase  = new OptionToMaybePhase(config)
        assertTrue(phase.runsBefore.contains("firstTransform"))
      }
    )

  private val phaseConfigurationSpec =
    suite("::phase configuration")(
      test("creates phase with default config") {
        val config = PluginConfig()
        val phase  = new OptionToMaybePhase(config)
        assertTrue(phase.phaseName == "option-to-maybe")
      },
      test("creates phase with verbose config") {
        val config = PluginConfig(verbose = true)
        val phase  = new OptionToMaybePhase(config)
        assertTrue(phase.phaseName == "option-to-maybe")
      },
      test("creates phase with aggressive config") {
        val config = PluginConfig(aggressive = true)
        val phase  = new OptionToMaybePhase(config)
        assertTrue(phase.phaseName == "option-to-maybe")
      },
      test("creates phase with preserve public API config") {
        val config = PluginConfig(preservePublicApi = true)
        val phase  = new OptionToMaybePhase(config)
        assertTrue(phase.phaseName == "option-to-maybe")
      },
      test("creates phase with package filters") {
        val config = PluginConfig(
          excludedPackages = Set("com.example"),
          includedPackages = Set("com.myapp")
        )
        val phase  = new OptionToMaybePhase(config)
        assertTrue(phase.phaseName == "option-to-maybe")
      }
    )

  private val phaseConfigVariationsSpec =
    suite("::phase config variations")(
      test("handles all config combinations") {
        val config = PluginConfig(
          verbose = true,
          aggressive = true,
          preservePublicApi = true,
          excludedPackages = Set("com.example", "org.test"),
          includedPackages = Set("com.myapp", "com.mylib")
        )
        val phase  = new OptionToMaybePhase(config)
        assertTrue(phase.phaseName == "option-to-maybe")
      },
      test("handles empty package filters") {
        val config = PluginConfig(
          excludedPackages = Set.empty,
          includedPackages = Set.empty
        )
        val phase  = new OptionToMaybePhase(config)
        assertTrue(phase.phaseName == "option-to-maybe")
      },
      test("handles single package filters") {
        val config1 = PluginConfig(excludedPackages = Set("com.example"))
        val phase1  = new OptionToMaybePhase(config1)

        val config2 = PluginConfig(includedPackages = Set("com.myapp"))
        val phase2  = new OptionToMaybePhase(config2)

        assertTrue(
          phase1.phaseName == "option-to-maybe",
          phase2.phaseName == "option-to-maybe"
        )
      }
    )

  private val phaseConsistencySpec =
    suite("::phase consistency")(
      test("multiple phases with same config have same properties") {
        val config = PluginConfig(verbose = true, excludedPackages = Set("com.test"))
        val phase1 = new OptionToMaybePhase(config)
        val phase2 = new OptionToMaybePhase(config)

        assertTrue(
          phase1.phaseName == phase2.phaseName,
          phase1.runsAfter == phase2.runsAfter,
          phase1.runsBefore == phase2.runsBefore
        )
      },
      test("phases with different configs have same basic properties") {
        val config1 = PluginConfig(verbose = true)
        val config2 = PluginConfig(aggressive = true)
        val phase1  = new OptionToMaybePhase(config1)
        val phase2  = new OptionToMaybePhase(config2)

        assertTrue(
          phase1.phaseName == phase2.phaseName,
          phase1.runsAfter == phase2.runsAfter,
          phase1.runsBefore == phase2.runsBefore
        )
      }
    )

  // Note: Testing the actual AST transformations (transformApply, transformSelect, etc.)
  // would require setting up a complete compiler context with typed trees.
  // These tests would be better suited for integration tests that compile actual code
  // with the plugin enabled. For now, we test the basic phase structure and configuration.

  private val phaseIntegrationNotesSpec =
    suite("::integration test notes")(
      test("phase structure is ready for AST transformation testing") {
        val config = PluginConfig()
        val phase  = new OptionToMaybePhase(config)

        // Verify that the phase has the transformation methods
        // (This is more of a structural test to ensure the methods exist)
        val clazz   = phase.getClass
        val methods = clazz.getDeclaredMethods.map(_.getName).toSet

        assertTrue(
          methods.contains("transformApply"),
          methods.contains("transformSelect"),
          methods.contains("transformTypeApply"),
          methods.contains("shouldTransform"),
          methods.contains("isOptionRelated"),
          methods.contains("transformOptionToMaybe"),
          methods.contains("transformOptionSelect"),
          methods.contains("transformOptionTypeApply")
        )
      }
    )

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("OptionToMaybePhaseSpec")(
      phaseBasicsSpec,
      phaseConfigurationSpec,
      phaseConfigVariationsSpec,
      phaseConsistencySpec,
      phaseIntegrationNotesSpec
    )
}
