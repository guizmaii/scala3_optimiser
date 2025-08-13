package com.guizmaii.scala.optimiser.plugin

import dotty.tools.dotc.ast.Trees.*
import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Names.*
import dotty.tools.dotc.core.StdNames.*
import dotty.tools.dotc.core.Symbols.*
import dotty.tools.dotc.core.Types.*
import dotty.tools.dotc.plugins.PluginPhase
import dotty.tools.dotc.transform.{Pickler, FirstTransform}

class OptionToMaybePhase(config: PluginConfig) extends PluginPhase {
  import tpd.*

  val phaseName = "option-to-maybe"

  override val runsAfter: Set[String]  = Set(Pickler.name)
  override val runsBefore: Set[String] = Set(FirstTransform.name)

  override def transformApply(tree: tpd.Apply)(using Context): tpd.Tree =
    if (shouldTransform(tree)) {
      transformOptionToMaybe(tree)
    } else {
      super.transformApply(tree)
    }

  override def transformSelect(tree: tpd.Select)(using Context): tpd.Tree =
    if (shouldTransform(tree)) {
      transformOptionSelect(tree)
    } else {
      super.transformSelect(tree)
    }

  override def transformTypeApply(tree: tpd.TypeApply)(using Context): tpd.Tree =
    if (shouldTransform(tree)) {
      transformOptionTypeApply(tree)
    } else {
      super.transformTypeApply(tree)
    }

  private def shouldTransform(tree: tpd.Tree)(using ctx: Context): Boolean = {
    // Check if transformation should be applied based on config
    val currentPackageOpt = ctx.owner.ownersIterator.find(_.isPackageObject).map(_.fullName.toString)

    val packageCheckResult = currentPackageOpt match {
      case Some(currentPackage) =>
        val excluded = config.excludedPackages.exists(pkg => isPackageMatch(currentPackage, pkg))
        val included = config.includedPackages.isEmpty || config.includedPackages.exists(pkg => isPackageMatch(currentPackage, pkg))
        !excluded && included
      case None                 =>
        // No package context found - apply transformation only if no specific inclusion rules are set
        config.includedPackages.isEmpty
    }

    packageCheckResult && isOptionRelated(tree)
  }

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

  private def isOptionRelated(tree: tpd.Tree)(using Context): Boolean =
    tree match {
      case Apply(fun, _)        =>
        isOptionSymbol(fun.symbol) || isOptionConstructor(fun)
      case Select(_, _)         =>
        isOptionSymbol(tree.symbol) || tree.tpe.derivesFrom(defn.OptionClass)
      case TypeApply(fun, args) =>
        isOptionSymbol(fun.symbol) || args.exists(_.tpe.derivesFrom(defn.OptionClass))
      case _                    => false
    }

  private def isOptionSymbol(sym: Symbol)(using Context): Boolean =
    sym.exists && (sym == defn.OptionClass || sym.owner == defn.OptionClass)

  private def isOptionConstructor(tree: tpd.Tree)(using Context): Boolean =
    tree match {
      case Select(qualifier, name) =>
        qualifier.symbol == defn.OptionClass && (name == nme.apply || name == termName("Some") || name == termName("None"))
      case Ident(name)             =>
        name == termName("Some") || name == termName("None")
      case _                       => false
    }

  private def transformOptionToMaybe(tree: tpd.Apply)(using Context): tpd.Tree = {
    if (config.verbose) {
      println(s"[option-optimizer] Transforming Option constructor: ${tree.show}")
    }

    tree.fun match {
      case Select(qualifier, name) if qualifier.symbol == defn.OptionClass =>
        // Option.apply(x) -> Maybe(x)
        transformToMaybeApply(tree.args)
      case Ident(name) if name == termName("Some")                         =>
        // Some(x) -> Maybe(x)
        transformToMaybeApply(tree.args)
      case Select(_, name) if name == termName("Some")                     =>
        // _.Some(x) -> Maybe(x)
        transformToMaybeApply(tree.args)
      case _                                                               => super.transformApply(tree)
    }
  }

  private def transformOptionSelect(tree: tpd.Select)(using Context): tpd.Tree = {
    if (config.verbose) {
      println(s"[option-optimizer] Transforming Option select: ${tree.show}")
    }

    tree match {
      case Select(qualifier, name) if name == termName("None") =>
        // None -> Maybe.empty
        createMaybeEmpty()
      case _                                                   => super.transformSelect(tree)
    }
  }

  private def transformOptionTypeApply(tree: tpd.TypeApply)(using Context): tpd.Tree = {
    if (config.verbose) {
      println(s"[option-optimizer] Transforming Option type application: ${tree.show}")
    }

    // Transform Option[T] type references to Maybe[T]
    val transformedFun  = transformOptionTypeRef(tree.fun)
    val transformedArgs = tree.args.map(transformOptionTypeInArg)

    if (transformedFun != tree.fun || transformedArgs != tree.args) {
      tpd.cpy.TypeApply(tree)(transformedFun, transformedArgs)
    } else {
      super.transformTypeApply(tree)
    }
  }

  private def transformOptionTypeRef(tree: tpd.Tree)(using Context): tpd.Tree =
    tree match {
      case Select(qualifier, name) if qualifier.symbol == defn.OptionClass =>
        createMaybeTypeRef()
      case Ident(name) if name == typeName("Option")                       =>
        createMaybeTypeRef()
      case _                                                               => tree
    }

  private def transformOptionTypeInArg(tree: tpd.Tree)(using Context): tpd.Tree =
    tree match {
      case tpt if tpt.tpe.derivesFrom(defn.OptionClass) =>
        // Transform Option[T] to Maybe[T] in type arguments
        createMaybeType(getOptionTypeArg(tpt.tpe))
      case _                                            => tree
    }

  private def getOptionTypeArg(tpe: Type)(using Context): Type =
    tpe match {
      case AppliedType(_, List(arg)) => arg
      case _                         => defn.AnyType
    }

  private def transformToMaybeApply(args: List[tpd.Tree])(using Context): tpd.Tree = {
    val maybeRef = createMaybeRef()
    tpd.Apply(maybeRef, args)
  }

  private def createMaybeRef()(using Context): tpd.Tree = {
    val kyoPkg      = requiredPackage("kyo")
    val maybeModule = kyoPkg.requiredValue("Maybe")
    tpd.ref(maybeModule)
  }

  private def createMaybeEmpty()(using Context): tpd.Tree = {
    val maybeRef = createMaybeRef()
    tpd.Select(maybeRef, termName("empty"))
  }

  private def createMaybeTypeRef()(using Context): tpd.Tree = {
    val kyoPkg     = requiredPackage("kyo")
    val maybeClass = kyoPkg.requiredType("Maybe")
    tpd.ref(maybeClass)
  }

  private def createMaybeType(argType: Type)(using Context): tpd.Tree = {
    val maybeTypeRef = createMaybeTypeRef()
    val argTree      = tpd.TypeTree(argType)
    tpd.AppliedTypeTree(maybeTypeRef, List(argTree))
  }
}
