package com.github.fburato.highwheelmodules.core.algorithms

import com.github.fburato.highwheelmodules.model.bytecode.{AccessPointS, AccessTypeS, ElementNameS}
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor
import com.github.fburato.highwheelmodules.model.modules.{AnonymousModuleS, HWModuleS, ModuleGraphS}

case class ModuleDependenciesGraphBuildingVisitor[T](modules: Seq[HWModuleS],
                                                     graph: ModuleGraphS[T],
                                                     other: HWModuleS,
                                                     dependencyBuilder: (HWModuleS, HWModuleS, AccessPointS, AccessPointS, AccessTypeS) => T,
                                                     whiteList: Option[AnonymousModuleS],
                                                     blacklist: Option[AnonymousModuleS]) extends AccessVisitor {

  private def addModulesToGraph(): Unit = {
    graph addModule other
    modules.foreach(m => graph addModule m)
  }

  addModulesToGraph()

  override def newNode(clazz: ElementNameS): Unit = ()

  override def newAccessPoint(ap: AccessPointS): Unit = ()

  override def newEntryPoint(clazz: ElementNameS): Unit = ()

  override def apply(source: AccessPointS, dest: AccessPointS, `type`: AccessTypeS): Unit = {
    def elementInWhiteListAndOutOfBlacklist(element: ElementNameS): Boolean =
      whiteList.forall(m => m contains element) && blacklist.forall(m => !(m contains element))

    def matchingModules(element: ElementNameS): Seq[HWModuleS] =
      modules.filter(m => m.contains(element))

    if (elementInWhiteListAndOutOfBlacklist(source.elementName) && elementInWhiteListAndOutOfBlacklist(dest.elementName)) {
      val modulesMatchingSource = matchingModules(source.elementName)
      val modulesMatchingDest = matchingModules(dest.elementName)
      for {
        sourceModule <- modulesMatchingSource
        destModule <- modulesMatchingDest
      } if (sourceModule != destModule) {
        graph addDependency dependencyBuilder(sourceModule, destModule, source, dest, `type`)
      }
      if (modulesMatchingSource.isEmpty && modulesMatchingDest.nonEmpty) {
        modulesMatchingDest.foreach(m => graph addDependency dependencyBuilder(other, m, source, dest, `type`))
      }
      if (modulesMatchingSource.nonEmpty && modulesMatchingDest.isEmpty) {
        modulesMatchingSource.foreach(m => graph addDependency dependencyBuilder(m, other, source, dest, `type`))
      }
    }
  }
}
