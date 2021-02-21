package com.github.fburato.highwheelmodules.core.analysis

trait Analyser {
  def analyse(state: AnalysisState): AnalysisResult
}
