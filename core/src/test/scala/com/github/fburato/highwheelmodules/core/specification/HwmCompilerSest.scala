package com.github.fburato.highwheelmodules.core.specification

import com.github.fburato.highwheelmodules.model.analysis.AnalysisMode
import com.github.fburato.highwheelmodules.model.modules.{AnonymousModule, HWModule, Definition => ModelDefinition}
import com.github.fburato.highwheelmodules.model.rules.{Dependency, NoStrictDependency}
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.jdk.CollectionConverters._

class HwmCompilerSest extends AnyWordSpec with Matchers with MockitoSugar with OneInstancePerTest {
  private val CORE = HWModule.make("core", "core").get
  private val COMMONS = HWModule.make("commons", "commons").get
  private val MAIN = HWModule.make("main", "main").get
  private val IO = HWModule.make("io", "io").get
  private val baseDefinition = Definition(None, None, None, None, List(), List())

  "compile minimal specification" in {
    val result = compile(baseDefinition.copy(
      moduleDefinitions = List(),
      rules = List()
    ))

    result.modules.isEmpty shouldBe true
    result.dependencies.isEmpty shouldBe true
    result.blackList.isPresent shouldBe false
    result.whitelist.isPresent shouldBe false
    result.mode shouldBe AnalysisMode.STRICT
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

    result.modules.asScala.toList should contain theSameElementsAs List(
      hwmModule("Foo", "org.example.foo.*", "org.example.foobar.*"),
      hwmModule("Bar", "org.example.bar.*")
    )
  }

  def hwmModule(name: String, globs: String*): HWModule =
    HWModule.make(name, globs: _*).get()

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

    result.modules.asScala.toList should contain theSameElementsAs List(
      CORE, COMMONS, MAIN, IO
    )
  }

  "convert chain dependencies in two-by-two dependencies" in {
    val result = compile(definitionWithModules.copy(
      rules = List(
        ChainDependencyRule(List("main", "core", "commons"))
      )
    ))

    result.dependencies.asScala.toList should contain theSameElementsAs List(
      new Dependency(MAIN, CORE),
      new Dependency(CORE, COMMONS)
    )
  }

  "convert one to many dependency in two-by-two dependencies" in {
    val result = compile(definitionWithModules.copy(
      rules = List(
        OneToManyRule("main", List("core", "commons"))
      )
    ))
    result.dependencies.asScala.toList should contain theSameElementsAs List(
      new Dependency(MAIN, CORE),
      new Dependency(MAIN, COMMONS)
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
    result.dependencies.asScala.toList should contain theSameElementsAs List(
      new Dependency(CORE, MAIN),
      new Dependency(COMMONS, MAIN)
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

    result.noStrictDependencies.asScala.toList should contain theSameElementsAs List(
      new NoStrictDependency(MAIN, COMMONS)
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

    result.blackList.get shouldBe AnonymousModule.make("c", "d").get
    result.whitelist.get shouldBe AnonymousModule.make("a", "b").get
    result.modules.asScala.toList should contain theSameElementsAs List(
      hwmModule("Foo", "org.example.foo.*"),
      hwmModule("Bar", "org.example.bar.*")
    )
  }

  "set mode to strict if specification does not contain module explicitly" in {
    val result = compile(baseDefinition)

    result.mode shouldBe AnalysisMode.STRICT
  }

  "set mode to strict if specification uses strict explicit as mode" in {
    val result = compile(baseDefinition.copy(
      mode = Some("STRICT")
    ))

    result.mode shouldBe AnalysisMode.STRICT
  }

  "set mode to loose if specification uses loose explicit as mode" in {
    val result = compile(baseDefinition.copy(
      mode = Some("LOOSE")
    ))

    result.mode shouldBe AnalysisMode.LOOSE
  }

  "fail to compile if mode is not recognised" in {
    HwmCompiler.compile(definitionWithModules.copy(
      mode = Some("SOMETHING")
    )).isLeft shouldBe true
  }
}
