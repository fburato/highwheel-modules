package com.github.fburato.highwheelmodules.core

import java.io.{File, FileReader, IOException}

import com.github.fburato.highwheelmodules.core.analysis.AnalyserException
import com.github.fburato.highwheelmodules.core.specification.Compiler
import com.github.fburato.highwheelmodules.core.specification.parsers.DefinitionParser
import com.github.fburato.highwheelmodules.model.modules.Definition

import scala.util.{Failure, Try}

trait SpecificationCompiler {
  def compile(file: File): Try[Definition]
}

object SpecificationCompiler {

  def apply(): SpecificationCompiler = new SpecificationCompiler {
    private val definitionParser = new DefinitionParser
    private val compiler = new Compiler

    override def compile(file: File): Try[Definition] = {
      Try {
        val definition = definitionParser parse new FileReader(file)
        compiler.compile(definition)
      } recoverWith {
        case e: IOException => Failure(AnalyserException(s"Error while parsing the specification file: '${e.getMessage}'"))
      }
    }
  }

}
