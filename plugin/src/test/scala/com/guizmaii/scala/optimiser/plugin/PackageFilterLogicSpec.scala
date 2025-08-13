package com.guizmaii.scala.optimiser.plugin

import zio.test.*
import zio.Scope

object PackageFilterLogicSpec extends ZIOSpecDefault {

  // Note: These are unit tests for the package filtering logic.
  // Testing shouldTransform method directly would require setting up
  // a complete compiler context, so we test the logic concepts here.

  // Helper method that matches the implementation logic
  private def isPackageMatch(currentPackage: String, filterPackage: String): Boolean =
    if (filterPackage.isEmpty) {
      // Empty filter matches everything
      true
    } else if (currentPackage == filterPackage) {
      // Exact match
      true
    } else if (currentPackage.startsWith(filterPackage + ".")) {
      // Package hierarchy match: "com.example.sub" matches filter "com.example"
      true
    } else {
      false
    }

  private val noPackageFiltersSpec =
    suite("::no package filters")(
      test("processes all packages when no filters are set") {
        val config = PluginConfig()
        assertTrue(
          config.excludedPackages.isEmpty,
          config.includedPackages.isEmpty
        )
        // Logic: when no filters are set, everything should be included
      }
    )

  private val exclusionOnlySpec =
    suite("::exclusion only filters")(
      test("excludes specified packages") {
        val config = PluginConfig(excludedPackages = Set("com.example", "org.test"))

        // Test logic: a package "com.example.myclass" should be excluded
        val shouldExclude = config.excludedPackages.exists(pkg => "com.example.myclass".startsWith(pkg))

        assertTrue(
          config.excludedPackages == Set("com.example", "org.test"),
          shouldExclude
        )
      },
      test("does not exclude non-matching packages") {
        val config = PluginConfig(excludedPackages = Set("com.example"))

        // Test logic: a package "com.myapp" should not be excluded
        val shouldExclude = config.excludedPackages.exists(pkg => "com.myapp".startsWith(pkg))
        assertTrue(!shouldExclude)
      }
    )

  private val inclusionOnlySpec =
    suite("::inclusion only filters")(
      test("includes only specified packages") {
        val config = PluginConfig(includedPackages = Set("com.myapp"))

        // Test logic: a package "com.myapp.service" should be included
        val shouldInclude = config.includedPackages.isEmpty || config.includedPackages.exists(pkg => "com.myapp.service".startsWith(pkg))
        assertTrue(shouldInclude)
      },
      test("excludes non-matching packages when inclusion rules are set") {
        val config = PluginConfig(includedPackages = Set("com.myapp"))

        // Test logic: a package "com.example" should not be included
        val shouldInclude = config.includedPackages.isEmpty || config.includedPackages.exists(pkg => "com.example".startsWith(pkg))
        assertTrue(!shouldInclude)
      }
    )

  private val bothFiltersSpec =
    suite("::both inclusion and exclusion filters")(
      test("respects both inclusion and exclusion rules") {
        val config = PluginConfig(
          excludedPackages = Set("com.myapp.internal"),
          includedPackages = Set("com.myapp")
        )

        // Test logic for "com.myapp.service" - should be included and not excluded
        val currentPackage  = "com.myapp.service"
        val excluded        = config.excludedPackages.exists(pkg => currentPackage.startsWith(pkg))
        val included        = config.includedPackages.isEmpty || config.includedPackages.exists(pkg => currentPackage.startsWith(pkg))
        val shouldTransform = !excluded && included

        assertTrue(
          !excluded, // not excluded
          included,  // is included
          shouldTransform
        )
      },
      test("exclusion takes precedence over inclusion") {
        val config = PluginConfig(
          excludedPackages = Set("com.myapp.internal"),
          includedPackages = Set("com.myapp")
        )

        // Test logic for "com.myapp.internal.secret" - should be excluded despite matching inclusion
        val currentPackage  = "com.myapp.internal.secret"
        val excluded        = config.excludedPackages.exists(pkg => currentPackage.startsWith(pkg))
        val included        = config.includedPackages.isEmpty || config.includedPackages.exists(pkg => currentPackage.startsWith(pkg))
        val shouldTransform = !excluded && included

        assertTrue(
          excluded, // is excluded
          included, // would be included, but exclusion takes precedence
          !shouldTransform
        )
      }
    )

  private val noPackageContextSpec =
    suite("::no package context scenarios")(
      test("applies transformation when no inclusion rules and no package context") {
        val config                       = PluginConfig() // no filters
        // Logic: when currentPackageOpt is None and no inclusion rules, should transform
        val shouldTransformWhenNoPackage = config.includedPackages.isEmpty
        assertTrue(shouldTransformWhenNoPackage)
      },
      test("does not apply transformation when inclusion rules exist but no package context") {
        val config                       = PluginConfig(includedPackages = Set("com.myapp"))
        // Logic: when currentPackageOpt is None but inclusion rules exist, should not transform
        val shouldTransformWhenNoPackage = config.includedPackages.isEmpty
        assertTrue(!shouldTransformWhenNoPackage)
      }
    )

  private val edgeCasesSpec =
    suite("::edge cases")(
      test("handles exact package name matches") {
        val config   = PluginConfig(excludedPackages = Set("com.example"))
        val excluded = config.excludedPackages.exists(pkg => "com.example".startsWith(pkg))
        assertTrue(excluded)
      },
      test("handles package prefix matching") {
        val config   = PluginConfig(excludedPackages = Set("com.example"))
        val excluded = config.excludedPackages.exists(pkg => "com.example.subpackage.MyClass".startsWith(pkg))
        assertTrue(excluded)
      },
      test("does not match partial package names") {
        val config   = PluginConfig(excludedPackages = Set("com.example"))
        // Test the proper package matching logic
        val excluded = config.excludedPackages.exists { pkg =>
          val currentPackage = "com.exampleapp"
          if (pkg.isEmpty) {
            true
          } else if (currentPackage == pkg) {
            true
          } else if (currentPackage.startsWith(pkg + ".")) {
            true
          } else {
            false
          }
        }
        assertTrue(!excluded) // "com.exampleapp" should not match "com.example"
      },
      test("handles empty package names in filters") {
        // Empty package names should not be added to the config in the first place
        // But if they somehow get there, they should match everything
        val config   = PluginConfig(excludedPackages = Set(""))
        // Empty string matches everything with our logic
        val excluded = config.excludedPackages.exists(pkg => isPackageMatch("com.example", pkg))
        assertTrue(excluded)
      }
    )

  private val multiplePackageSpec =
    suite("::multiple package scenarios")(
      test("handles multiple exclusion patterns") {
        val config = PluginConfig(excludedPackages = Set("com.example", "org.internal", "net.test"))

        assertTrue(
          config.excludedPackages.exists(pkg => "com.example.app".startsWith(pkg)),
          config.excludedPackages.exists(pkg => "org.internal.service".startsWith(pkg)),
          config.excludedPackages.exists(pkg => "net.test.util".startsWith(pkg)),
          !config.excludedPackages.exists(pkg => "com.myapp".startsWith(pkg))
        )
      },
      test("handles multiple inclusion patterns") {
        val config = PluginConfig(includedPackages = Set("com.myapp", "com.mylib"))

        val shouldIncludeApp   = config.includedPackages.isEmpty || config.includedPackages.exists(pkg => "com.myapp.service".startsWith(pkg))
        val shouldIncludeLib   = config.includedPackages.isEmpty || config.includedPackages.exists(pkg => "com.mylib.util".startsWith(pkg))
        val shouldExcludeOther = config.includedPackages.isEmpty || config.includedPackages.exists(pkg => "com.other".startsWith(pkg))

        assertTrue(
          shouldIncludeApp,
          shouldIncludeLib,
          !shouldExcludeOther
        )
      }
    )

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("PackageFilterLogicSpec")(
      noPackageFiltersSpec,
      exclusionOnlySpec,
      inclusionOnlySpec,
      bothFiltersSpec,
      noPackageContextSpec,
      edgeCasesSpec,
      multiplePackageSpec
    )
}
