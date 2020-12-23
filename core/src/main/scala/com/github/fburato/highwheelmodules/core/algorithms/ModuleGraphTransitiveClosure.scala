package com.github.fburato.highwheelmodules.core.algorithms


import com.github.fburato.highwheelmodules.core.algorithms.ModuleGraphTransitiveClosure.{Difference, PathDifference}
import com.github.fburato.highwheelmodules.model.modules.{HWModule, ModuleDependency, ModuleGraph}

import scala.collection.mutable.ArrayBuffer

/**
 * Calculate the Transitive closure of a ModuleGraph using the Floyd-Warshall algorithm Ref Cormen, Thomas H.;
 * Leiserson, Charles E.; Rivest, Ronald L. (1990). Introduction to Algorithms (1st ed.). MIT Press and McGraw-Hill.
 * ISBN 0-262-03141-8. See in particular Section 26.2, "The Floyd–Warshall algorithm" pp. 558–565
 */
class ModuleGraphTransitiveClosure(
                                    private val minimumPathMatrix: Array[Array[ArrayBuffer[HWModule]]],
                                    private val indexMap: Map[HWModule, Int],
                                    private val modules: Seq[HWModule]
                                  ) {
  def isReachable(vertex1: HWModule, vertex2: HWModule): Boolean =
    minimumDistance(vertex1, vertex2).exists(d => d < Int.MaxValue)

  def same(other: ModuleGraphTransitiveClosure): Boolean =
    diff(other).exists(s => s.isEmpty)

  def diff(other: ModuleGraphTransitiveClosure): Option[Seq[ModuleGraphTransitiveClosure.Difference]] =
    for {
      pathDiff <- diffPath(other)
      transformed = pathDiff.map(pDiff => Difference(pDiff.source, pDiff.dest, pDiff.firstPath.size, pDiff.secondPath.size))
    } yield transformed

  def diffPath(other: ModuleGraphTransitiveClosure): Option[Seq[ModuleGraphTransitiveClosure.PathDifference]] = {
    def computePathDiff: Seq[ModuleGraphTransitiveClosure.PathDifference] =
      for {
        i <- modules
        j <- modules
        thisI = indexMap(i)
        thisJ = indexMap(j)
        otherI = other.indexMap(i)
        otherJ = other.indexMap(j)
        res = PathDifference(i, j, minimumPathMatrix(thisI)(thisJ).toSeq, other.minimumPathMatrix(otherI)(otherJ).toSeq)
        if minimumPathMatrix(thisI)(thisJ).size != other.minimumPathMatrix(otherI)(otherJ).size
      } yield res

    if (!modules.forall(m => other.modules.contains(m)) || !other.modules.forall(m => modules.contains(m))) {
      None
    } else {
      Some(computePathDiff)
    }
  }

  def minimumDistance(vertex1: HWModule, vertex2: HWModule): Option[Int] =
    for {
      v1Index <- indexMap.get(vertex1)
      v2Index <- indexMap.get(vertex2)
      distance = minimumPathMatrix(v1Index)(v2Index).size
    } yield if (distance == 0) Int.MaxValue else distance

  def minimumDistancePath(vertex1: HWModule, vertex2: HWModule): Seq[HWModule] =
    (for {
      v1Index <- indexMap.get(vertex1)
      v2Index <- indexMap.get(vertex2)
    } yield minimumPathMatrix(v1Index)(v2Index).toSeq).getOrElse(Seq())
}

object ModuleGraphTransitiveClosure {

  case class Difference(source: HWModule, dest: HWModule, firstDistance: Int, secondDistance: Int)

  case class PathDifference(source: HWModule, dest: HWModule, firstPath: Seq[HWModule], secondPath: Seq[HWModule])

  def apply(moduleGraph: ModuleGraph[ModuleDependency], modulesSeq: Seq[HWModule]): ModuleGraphTransitiveClosure = {
    val minimumPathMatrix = Array.ofDim[ArrayBuffer[HWModule]](modulesSeq.size, modulesSeq.size)
    for {
      i <- modulesSeq.indices
      j <- modulesSeq.indices
    } {
      minimumPathMatrix(i)(j) = new ArrayBuffer[HWModule]()
    }
    val mapModuleIndex = modulesSeq.zipWithIndex.toMap
    for {
      m <- modulesSeq
      dependency <- moduleGraph.dependencies(m)
    } {
      minimumPathMatrix(mapModuleIndex(m))(mapModuleIndex(dependency)) += dependency
    }
    applyFloydWarshallMainIteration(minimumPathMatrix, modulesSeq.size)
    new ModuleGraphTransitiveClosure(minimumPathMatrix, mapModuleIndex, modulesSeq)
  }

  private def applyFloydWarshallMainIteration(minimumPathMatrix: Array[Array[ArrayBuffer[HWModule]]], size: Int): Unit = {
    for {
      i <- 0 until size
      j <- 0 until size
      k <- 0 until size
    } {
      val minimumIJ = minimumPathMatrix(i)(j)
      val minimumIK = minimumPathMatrix(i)(k)
      val minimumKJ = minimumPathMatrix(k)(j)
      if (minimumIJ.isEmpty && minimumIK.nonEmpty && minimumKJ.nonEmpty
        || minimumIJ.nonEmpty && minimumIK.nonEmpty && minimumKJ.nonEmpty && (minimumIK.size + minimumKJ.size < minimumIJ.size)) {
        minimumPathMatrix(i)(j) = minimumIK ++ minimumKJ
      }
    }
  }
}
