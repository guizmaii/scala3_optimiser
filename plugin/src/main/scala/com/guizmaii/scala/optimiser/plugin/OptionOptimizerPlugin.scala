package com.guizmaii.scala.optimiser.plugin

import dotty.tools.dotc.plugins.{PluginPhase, StandardPlugin}

class OptionOptimizerPlugin extends StandardPlugin {
  val name: String        = "option-optimizer"
  val description: String = "Scala 3 compiler plugin that replaces Option with Kyo's Maybe for zero-allocation performance"

  @annotation.nowarn("cat=deprecation")
  override def init(options: List[String]): List[PluginPhase] = {
    val config = PluginConfig.parse(options)
    List(new OptionToMaybePhase(config))
  }
}

final case class PluginConfig(
  verbose: Boolean = false,
  aggressive: Boolean = false,
  preservePublicApi: Boolean = false,
  excludedPackages: Set[String] = Set.empty,
  includedPackages: Set[String] = Set.empty
)

object PluginConfig {
  def parse(options: List[String]): PluginConfig =
    options.foldLeft(PluginConfig()) { (config, option) =>
      option match {
        case "verbose"             => config.copy(verbose = true)
        case "aggressive"          => config.copy(aggressive = true)
        case "preserve-public-api" => config.copy(preservePublicApi = true)
        case s"exclude:$pkg"       =>
          if (pkg.nonEmpty) {
            config.copy(excludedPackages = config.excludedPackages + pkg)
          } else {
            println(s"[option-optimizer] Invalid exclude option: empty package name")
            config
          }
        case s"include:$pkg"       =>
          if (pkg.nonEmpty) {
            config.copy(includedPackages = config.includedPackages + pkg)
          } else {
            println(s"[option-optimizer] Invalid include option: empty package name")
            config
          }
        case unknown               =>
          println(s"[option-optimizer] Unknown option: $unknown")
          config
      }
    }
}
