package com.github.fburato.highwheelmodules.core

import com.github.fburato.highwheelmodules.core.specification.HwmCompiler
import com.github.fburato.highwheelmodules.core.specification.lexer.HwmLexer
import com.github.fburato.highwheelmodules.core.specification.parser.HwmParser
import com.github.fburato.highwheelmodules.model.modules.Definition

import java.io.File
import java.nio.charset.StandardCharsets
import scala.io.Source
import scala.util.Try
import scala.util.parsing.input.StreamReader

trait SpecificationCompiler {
  def compile(file: File): Try[Definition]
}

object SpecificationCompiler {

  def apply(): SpecificationCompiler = (file: File) =>
    (for {
      maybeTokens <- Try(
        HwmLexer(StreamReader(Source.fromFile(file, StandardCharsets.UTF_8.toString).reader()))
      ).toEither
      tokens <- maybeTokens
      parsedDefinition <- HwmParser.parse(tokens)
      definition <- HwmCompiler.compile(parsedDefinition)
    } yield definition).toTry

}
