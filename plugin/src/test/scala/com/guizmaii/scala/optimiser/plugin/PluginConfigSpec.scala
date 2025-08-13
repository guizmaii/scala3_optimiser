package com.guizmaii.scala.optimiser.plugin

import zio.test.*
import zio.Scope

object PluginConfigSpec extends ZIOSpecDefault {

  private val defaultConfigSpec =
    suite("::default config")(
      test("creates default configuration with correct values") {
        val config = PluginConfig()
        assertTrue(
          !config.verbose,
          !config.aggressive,
          !config.preservePublicApi,
          config.excludedPackages.isEmpty,
          config.includedPackages.isEmpty
        )
      }
    )

  private val parseEmptyOptionsSpec =
    suite("::parse with empty options")(
      test("returns default configuration for empty options list") {
        val config = PluginConfig.parse(List.empty)
        assertTrue(
          !config.verbose,
          !config.aggressive,
          !config.preservePublicApi,
          config.excludedPackages.isEmpty,
          config.includedPackages.isEmpty
        )
      }
    )

  private val parseSingleOptionsSpec =
    suite("::parse with single options")(
      test("parses verbose option") {
        val config = PluginConfig.parse(List("verbose"))
        assertTrue(
          config.verbose,
          !config.aggressive,
          !config.preservePublicApi
        )
      },
      test("parses aggressive option") {
        val config = PluginConfig.parse(List("aggressive"))
        assertTrue(
          !config.verbose,
          config.aggressive,
          !config.preservePublicApi
        )
      },
      test("parses preserve-public-api option") {
        val config = PluginConfig.parse(List("preserve-public-api"))
        assertTrue(
          !config.verbose,
          !config.aggressive,
          config.preservePublicApi
        )
      }
    )

  private val parsePackageOptionsSpec =
    suite("::parse with package options")(
      test("parses single exclude package") {
        val config = PluginConfig.parse(List("exclude:com.example"))
        assertTrue(
          config.excludedPackages == Set("com.example"),
          config.includedPackages.isEmpty
        )
      },
      test("parses multiple exclude packages") {
        val config = PluginConfig.parse(List("exclude:com.example", "exclude:org.test"))
        assertTrue(
          config.excludedPackages == Set("com.example", "org.test"),
          config.includedPackages.isEmpty
        )
      },
      test("parses single include package") {
        val config = PluginConfig.parse(List("include:com.myapp"))
        assertTrue(
          config.excludedPackages.isEmpty,
          config.includedPackages == Set("com.myapp")
        )
      },
      test("parses multiple include packages") {
        val config = PluginConfig.parse(List("include:com.myapp", "include:com.mylib"))
        assertTrue(
          config.excludedPackages.isEmpty,
          config.includedPackages == Set("com.myapp", "com.mylib")
        )
      },
      test("parses mixed include and exclude packages") {
        val options = List("include:com.myapp", "exclude:com.example", "include:com.mylib", "exclude:org.test")
        val config  = PluginConfig.parse(options)
        assertTrue(
          config.excludedPackages == Set("com.example", "org.test"),
          config.includedPackages == Set("com.myapp", "com.mylib")
        )
      }
    )

  private val parseCombinedOptionsSpec =
    suite("::parse with combined options")(
      test("parses all boolean options together") {
        val config = PluginConfig.parse(List("verbose", "aggressive", "preserve-public-api"))
        assertTrue(
          config.verbose,
          config.aggressive,
          config.preservePublicApi
        )
      },
      test("parses boolean and package options together") {
        val options = List("verbose", "exclude:com.example", "aggressive", "include:com.myapp")
        val config  = PluginConfig.parse(options)
        assertTrue(
          config.verbose,
          config.aggressive,
          !config.preservePublicApi,
          config.excludedPackages == Set("com.example"),
          config.includedPackages == Set("com.myapp")
        )
      }
    )

  private val parseUnknownOptionsSpec =
    suite("::parse with unknown options")(
      test("ignores unknown options and returns default values") {
        val config = PluginConfig.parse(List("unknown", "invalid-option"))
        assertTrue(
          !config.verbose,
          !config.aggressive,
          !config.preservePublicApi,
          config.excludedPackages.isEmpty,
          config.includedPackages.isEmpty
        )
      },
      test("processes known options while ignoring unknown ones") {
        val config = PluginConfig.parse(List("verbose", "unknown", "aggressive", "invalid-option"))
        assertTrue(
          config.verbose,
          config.aggressive,
          !config.preservePublicApi
        )
      }
    )

  private val parseEdgeCasesSpec =
    suite("::parse edge cases")(
      test("handles empty package names in exclude") {
        val config = PluginConfig.parse(List("exclude:"))
        assertTrue(config.excludedPackages.isEmpty)
      },
      test("handles empty package names in include") {
        val config = PluginConfig.parse(List("include:"))
        assertTrue(config.includedPackages.isEmpty)
      },
      test("handles malformed package options") {
        val config = PluginConfig.parse(List("exclude", "include"))
        assertTrue(
          config.excludedPackages.isEmpty,
          config.includedPackages.isEmpty
        )
      },
      test("processes duplicate options correctly") {
        val config = PluginConfig.parse(List("verbose", "verbose", "exclude:com.example", "exclude:com.example"))
        assertTrue(
          config.verbose,
          config.excludedPackages == Set("com.example")
        )
      }
    )

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("PluginConfigSpec")(
      defaultConfigSpec,
      parseEmptyOptionsSpec,
      parseSingleOptionsSpec,
      parsePackageOptionsSpec,
      parseCombinedOptionsSpec,
      parseUnknownOptionsSpec,
      parseEdgeCasesSpec
    )
}
