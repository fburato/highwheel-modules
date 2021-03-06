package com.github.fburato.highwheelmodules.core.specification

import com.github.fburato.highwheelmodules.model.analysis.{AnalysisMode, LOOSE, STRICT}
import com.github.fburato.highwheelmodules.model.modules.{
  AnonymousModule,
  HWModule,
  Definition => ModelDefinition
}
import com.github.fburato.highwheelmodules.model.rules.{DependencyS, NoStrictDependencyS}

object HwmCompiler {

  private val MODULE_HAS_NOT_BEEN_DEFINED =
    "HWModule '%s' referenced in rule '%s' has not been defined"

  def compile(definition: Definition): Either[CompilerException, ModelDefinition] = {
    val prefix = definition.prefix.getOrElse("")
    for {
      moduleMap <- compileModules(definition.moduleDefinitions, prefix)
      rules <- compileRules(definition.rules, moduleMap)
      whitelist <- definition.whitelist map (regexes =>
        compileRegexes(regexes).map(Some(_))
      ) getOrElse Right(None)
      blacklist <- definition.blacklist map (regexes =>
        compileRegexes(regexes).map(Some(_))
      ) getOrElse Right(None)
      mode <- definition.mode map compileMode getOrElse Right(STRICT)
    } yield ModelDefinition(
      mode = mode,
      blacklist = blacklist,
      whitelist = whitelist,
      modules = moduleMap.values.toSeq,
      dependencies = rules._1,
      noStrictDependencies = rules._2
    )
  }

  private def compileModules(
    modules: List[ModuleDefinition],
    prefix: String
  ): Either[CompilerException, Map[String, HWModule]] = {
    val compiled: List[Either[CompilerException, HWModule]] = modules.map { module =>
      HWModule
        .make(module.moduleName, module.moduleRegex.map(regex => prefix + regex))
        .map(hwm => Right(hwm))
        .getOrElse(
          Left(
            CompilerException(
              s"Regular expressions '${module.moduleRegex}' for module ${module.moduleName} are not well defined"
            )
          )
        )
    }

    def checkRepetitions(
      modules: Map[String, List[HWModule]]
    ): Either[CompilerException, Map[String, HWModule]] = {
      val multiples = modules
        .filter(pair => pair._2.size > 1)
        .keys
      if (multiples.nonEmpty) {
        Left(
          CompilerException(
            s"The following modules appear multiple times: ${multiples.mkString(",")}"
          )
        )
      } else {
        Right(modules.map({ case (name, modules) =>
          name -> modules.head
        }))
      }
    }

    for {
      sequenced <- sequence(compiled)
      grouped <- Right(sequenced.groupBy(_.name))
      unrepeated <- checkRepetitions(grouped)
    } yield unrepeated
  }

  private def sequence[A, B](s: List[Either[A, B]]): Either[A, List[B]] =
    s.foldRight(Right(Nil): Either[A, List[B]]) { (e, acc) =>
      for {
        xs <- acc
        x <- e
      } yield x :: xs
    }

  private def compileRules(
    rules: List[Rule],
    moduleMap: Map[String, HWModule]
  ): Either[CompilerException, (List[DependencyS], List[NoStrictDependencyS])] = {
    def expandChainDependencyRule(
      ms: List[String]
    ): Either[CompilerException, List[DependencyS]] = {
      val maybeDependencies = ms.zip(ms.tail).map { case (m1, m2) =>
        if (!moduleMap.contains(m1)) {
          Left(CompilerException(String.format(MODULE_HAS_NOT_BEEN_DEFINED, m1, s"$m1->$m2")))
        } else if (!moduleMap.contains(m2)) {
          Left(CompilerException(String.format(MODULE_HAS_NOT_BEEN_DEFINED, m2, s"$m1->$m2")))
        } else {
          Right(DependencyS(moduleMap(m1), moduleMap(m2)))
        }
      }
      sequence(maybeDependencies)
    }

    def expandOneToManyRule(
      m: String,
      ms: List[String]
    ): Either[CompilerException, List[DependencyS]] = {
      val failure: String => CompilerException = f =>
        CompilerException(
          String.format(MODULE_HAS_NOT_BEEN_DEFINED, f, s"$m->(${ms.mkString(",")})")
        )
      if (!moduleMap.contains(m)) {
        Left(failure(m))
      } else {
        val maybeDependencies = ms map { other =>
          if (!moduleMap.contains(other)) {
            Left(failure(other))
          } else {
            Right(DependencyS(moduleMap(m), moduleMap(other)))
          }
        }
        sequence(maybeDependencies)
      }
    }

    def expandManyToOneRule(
      ms: List[String],
      m: String
    ): Either[CompilerException, List[DependencyS]] = {
      val failure: String => CompilerException = f =>
        CompilerException(
          String.format(MODULE_HAS_NOT_BEEN_DEFINED, f, s"(${ms.mkString(",")})->$m")
        )
      if (!moduleMap.contains(m)) {
        Left(failure(m))
      } else {
        val maybeDependencies = ms map { other =>
          if (!moduleMap.contains(other)) {
            Left(failure(other))
          } else {
            Right(DependencyS(moduleMap(other), moduleMap(m)))
          }
        }
        sequence(maybeDependencies)
      }
    }

    def compileNoDependent(
      left: String,
      right: String
    ): Either[CompilerException, List[NoStrictDependencyS]] = {
      val failure: String => CompilerException = f =>
        CompilerException(String.format(MODULE_HAS_NOT_BEEN_DEFINED, f, s"$left-/->$right"))
      if (!moduleMap.contains(left)) {
        Left(failure(left))
      } else if (!moduleMap.contains(right)) {
        Left(failure(right))
      } else {
        Right(List(NoStrictDependencyS(moduleMap(left), moduleMap(right))))
      }
    }

    def compileSingleRule(
      rule: Rule
    ): Either[CompilerException, (List[DependencyS], List[NoStrictDependencyS])] = rule match {
      case ChainDependencyRule(moduleNameChain) =>
        expandChainDependencyRule(moduleNameChain).map(d => (d, Nil))
      case ManyToOneRule(many, one)     => expandManyToOneRule(many, one).map(d => (d, Nil))
      case OneToManyRule(one, many)     => expandOneToManyRule(one, many).map(d => (d, Nil))
      case NoDependentRule(left, right) => compileNoDependent(left, right).map(d => (Nil, d))
    }

    val rulesSequenced = sequence(rules.map(compileSingleRule))
    rulesSequenced.map { l =>
      l.foldRight((Nil, Nil): (List[DependencyS], List[NoStrictDependencyS])) {
        case ((deps, noStrictDeps), (accDeps, accNoStrictDeps)) =>
          (deps ::: accDeps, noStrictDeps ::: accNoStrictDeps)
      }
    }
  }

  private def compileRegexes(regexes: List[String]): Either[CompilerException, AnonymousModule] =
    AnonymousModule.make(regexes) map (Right(_)) getOrElse Left(
      CompilerException(s"Regular expressions '${regexes.mkString(",")}' are not well defined")
    )

  private def compileMode(mode: String): Either[CompilerException, AnalysisMode] = mode match {
    case "STRICT" => Right(STRICT)
    case "LOOSE"  => Right(LOOSE)
    case _        => Left(CompilerException(s"Mode '$mode' is not recognised"))
  }
}
