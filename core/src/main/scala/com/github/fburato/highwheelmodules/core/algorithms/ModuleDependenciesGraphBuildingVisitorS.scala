package com.github.fburato.highwheelmodules.core.algorithms

import com.github.fburato.highwheelmodules.model.bytecode.{AccessPoint, AccessType, ElementName}
import com.github.fburato.highwheelmodules.model.classpath.AccessVisitor
import com.github.fburato.highwheelmodules.model.modules.{AnonymousModule, HWModule, ModuleGraph}

case class ModuleDependenciesGraphBuildingVisitorS[T](modules: Seq[HWModule],
                                                      graph: ModuleGraph[T],
                                                      other: HWModule,
                                                      dependencyBuilder: (HWModule, HWModule, AccessPoint, AccessPoint, AccessType) => T,
                                                      whiteList: Option[AnonymousModule],
                                                      blacklist: Option[AnonymousModule]) extends AccessVisitor {

  private def addModulesToGraph(): Unit = {
    graph addModule other
    modules.foreach(m => graph addModule m)
  }

  addModulesToGraph()

  override def newNode(clazz: ElementName): Unit = ()

  override def newAccessPoint(ap: AccessPoint): Unit = ()

  override def newEntryPoint(clazz: ElementName): Unit = ()

  override def apply(source: AccessPoint, dest: AccessPoint, `type`: AccessType): Unit = {
    def elementInWhiteListAndOutOfBlacklist(element: ElementName): Boolean =
      whiteList.forall(m => m contains element) && blacklist.forall(m => !(m contains element))

    def matchingModules(element: ElementName): Seq[HWModule] =
      modules.filter(m => m.contains(element))

    if (elementInWhiteListAndOutOfBlacklist(source.getElementName) && elementInWhiteListAndOutOfBlacklist(dest.getElementName)) {
      val modulesMatchingSource = matchingModules(source.getElementName)
      val modulesMatchingDest = matchingModules(dest.getElementName)
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
