package com.github.fburato.highwheelmodules.core.specification

import com.github.fburato.highwheelmodules.model.analysis.{LOOSE, STRICT}
import com.github.fburato.highwheelmodules.model.modules.{AnonymousModuleS, HWModuleS, DefinitionS => ModelDefinition}
import com.github.fburato.highwheelmodules.model.rules.{DependencyS, NoStrictDependencyS}
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HwmCompilerSest extends AnyWordSpec with Matchers with MockitoSugar with OneInstancePerTest {
  private val CORE = HWModuleS.make("core", Seq("core")).get
  private val COMMONS = HWModuleS.make("commons", Seq("commons")).get
  private val MAIN = HWModuleS.make("main", Seq("main")).get
  private val IO = HWModuleS.make("io", Seq("io")).get
  private val baseDefinition = Definition(None, None, None, None, List(), List())

  "compile minimal specification" in {
    val result = compile(baseDefinition.copy(
      moduleDefinitions = List(),
      rules = List()
    ))

    result.modules.isEmpty shouldBe true
    result.dependencies.isEmpty shouldBe true
    result.blacklist.isDefined shouldBe false
    result.whitelist.isDefined shouldBe false
    result.mode shouldBe STRICT
    result.noStrictDependencies.isEmpty shouldBe true
  }

  def compile(definition: Definition): ModelDefinition =
    HwmCompiler.compile(definition).getOrElse(throw new RuntimeException("should be right"))

  "fail if module regular expression fails to parse" in {
    HwmCompiler.compile(baseDefinition.copy(
      moduleDefinitions = List(ModuleDefinition("name", List("invalidregex[")))
    )).isLeft shouldBe true
  }


  "fail if module name appears twice" in {
    HwmCompiler.compile(baseDefinition.copy(
      moduleDefinitions = List(
        ModuleDefinition("name", List("name")),
        ModuleDefinition("name", List("name1"))
      )
    )).isLeft shouldBe true
  }

  "fail if prefix and modules are not a valid regex" in {
    HwmCompiler.compile(baseDefinition.copy(
      prefix = Some("invalidregex["),
      moduleDefinitions = List(
        ModuleDefinition("name", List("name")),
        ModuleDefinition("name", List("name1"))
      )
    )).isLeft shouldBe true
  }

  "fail if prefix is valid and module regex is invalid" in {
    HwmCompiler.compile(baseDefinition.copy(
      prefix = Some("prefix"),
      moduleDefinitions = List(
        ModuleDefinition("name", List("invalidregex["))
      )
    )).isLeft shouldBe true
  }

  "add prefix to all modules regexes" in {
    val result = compile(baseDefinition.copy(
      prefix = Some("org.example."),
      moduleDefinitions = List(
        ModuleDefinition("Foo", List("foo.*", "foobar.*")),
        ModuleDefinition("Bar", List("bar.*"))
      ),
      rules = List(
        ChainDependencyRule(List("Foo", "Bar"))
      )
    ))

    result.modules should contain theSameElementsAs List(
      hwmModule("Foo", "org.example.foo.*", "org.example.foobar.*"),
      hwmModule("Bar", "org.example.bar.*")
    )
  }

  def hwmModule(name: String, globs: String*): HWModuleS =
    HWModuleS.make(name, globs.toList).get

  "fail if rules refer to undefined modules" in {
    HwmCompiler.compile(baseDefinition.copy(
      moduleDefinitions = List(
        ModuleDefinition("name", List("name"))
      ),
      rules = List(
        ChainDependencyRule(List("name", "NOT_NAME"))
      )
    )).isLeft shouldBe true
  }

  private val definitionWithModules = baseDefinition.copy(
    moduleDefinitions = List(
      ModuleDefinition("core", List("core")),
      ModuleDefinition("commons", List("commons")),
      ModuleDefinition("main", List("main")),
      ModuleDefinition("io", List("io"))
    )
  )

  "return the expected modules" in {
    val result = compile(definitionWithModules)

    result.modules should contain theSameElementsAs List(
      CORE, COMMONS, MAIN, IO
    )
  }

  "convert chain dependencies in two-by-two dependencies" in {
    val result = compile(definitionWithModules.copy(
      rules = List(
        ChainDependencyRule(List("main", "core", "commons"))
      )
    ))

    result.dependencies should contain theSameElementsAs List(
      DependencyS(MAIN, CORE),
      DependencyS(CORE, COMMONS)
    )
  }

  "convert one to many dependency in two-by-two dependencies" in {
    val result = compile(definitionWithModules.copy(
      rules = List(
        OneToManyRule("main", List("core", "commons"))
      )
    ))
    result.dependencies should contain theSameElementsAs List(
      DependencyS(MAIN, CORE),
      DependencyS(MAIN, COMMONS)
    )
  }

  "fail to compile one to many if starting module does not exist" in {
    HwmCompiler.compile(definitionWithModules.copy(
      rules = List(
        OneToManyRule("NOT_MAIN", List("core", "commons"))
      )
    )).isLeft shouldBe true
  }

  "fail to compile one to many if ending module does not exist" in {
    HwmCompiler.compile(definitionWithModules.copy(
      rules = List(
        OneToManyRule("main", List("NOT_core", "commons"))
      )
    )).isLeft shouldBe true

    HwmCompiler.compile(definitionWithModules.copy(
      rules = List(
        OneToManyRule("main", List("core", "NOT_commons"))
      )
    )).isLeft shouldBe true
  }

  "convert many to one in two-by-two dependencies" in {
    val result = compile(definitionWithModules.copy(
      rules = List(
        ManyToOneRule(List("core", "commons"), "main")
      )
    ))
    result.dependencies should contain theSameElementsAs List(
      DependencyS(CORE, MAIN),
      DependencyS(COMMONS, MAIN)
    )
  }

  "fail to compile many to one if starting module does not exist" in {
    HwmCompiler.compile(definitionWithModules.copy(
      rules = List(
        ManyToOneRule(List("NOT_core", "commons"), "main")
      )
    )).isLeft shouldBe true

    HwmCompiler.compile(definitionWithModules.copy(
      rules = List(
        ManyToOneRule(List("core", "NOT_commons"), "main")
      )
    )).isLeft shouldBe true
  }

  "fail to compile many to one if ending module does not exist" in {
    HwmCompiler.compile(definitionWithModules.copy(
      rules = List(
        ManyToOneRule(List("core", "commons"), "NOT_main")
      )
    )).isLeft shouldBe true
  }

  "convert NoDependentRule appropriately" in {
    val result = compile(definitionWithModules.copy(
      rules = List(
        NoDependentRule("main", "commons")
      )
    ))

    result.noStrictDependencies should contain theSameElementsAs List(
      NoStrictDependencyS(MAIN, COMMONS)
    )
  }

  "fail if whitelist contains malformed regex" in {
    HwmCompiler.compile(definitionWithModules.copy(
      whitelist = Some(List("whitelist", "invalidregex["))
    )).isLeft shouldBe true
  }

  "fail if blacklist contains malformed regex" in {
    HwmCompiler.compile(definitionWithModules.copy(
      blacklist = Some(List("whitelist", "invalidregex["))
    )).isLeft shouldBe true
  }

  "compile definition with whitelist, blacklist and prefix" in {
    val result = compile(baseDefinition.copy(
      prefix = Some("org.example."),
      whitelist = Some(List("a", "b")),
      blacklist = Some(List("c", "d")),
      moduleDefinitions = List(
        ModuleDefinition("Foo", List("foo.*")),
        ModuleDefinition("Bar", List("bar.*"))
      ),
      rules = List(
        ChainDependencyRule(List("Foo", "Bar"))
      )
    ))

    result.blacklist.get shouldBe AnonymousModuleS.make(Seq("c", "d")).get
    result.whitelist.get shouldBe AnonymousModuleS.make(Seq("a", "b")).get
    result.modules should contain theSameElementsAs List(
      hwmModule("Foo", "org.example.foo.*"),
      hwmModule("Bar", "org.example.bar.*")
    )
  }

  "set mode to strict if specification does not contain module explicitly" in {
    val result = compile(baseDefinition)

    result.mode shouldBe STRICT
  }

  "set mode to strict if specification uses strict explicit as mode" in {
    val result = compile(baseDefinition.copy(
      mode = Some("STRICT")
    ))

    result.mode shouldBe STRICT
  }

  "set mode to loose if specification uses loose explicit as mode" in {
    val result = compile(baseDefinition.copy(
      mode = Some("LOOSE")
    ))

    result.mode shouldBe LOOSE
  }

  "fail to compile if mode is not recognised" in {
    HwmCompiler.compile(definitionWithModules.copy(
      mode = Some("SOMETHING")
    )).isLeft shouldBe true
  }
}
