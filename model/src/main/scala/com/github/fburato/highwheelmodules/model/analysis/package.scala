package com.github.fburato.highwheelmodules.model

package object analysis {

  sealed trait AnalysisMode

  case object STRICT extends AnalysisMode

  case object LOOSE extends AnalysisMode

}
