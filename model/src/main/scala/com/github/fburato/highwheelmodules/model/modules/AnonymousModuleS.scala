package com.github.fburato.highwheelmodules.model.modules

import com.github.fburato.highwheelmodules.model.bytecode.ElementNameS
import com.github.fburato.highwheelmodules.model.modules.AnonymousModuleS.globToRegex

import java.util.regex.{Pattern, PatternSyntaxException}
import scala.util.Try

case class AnonymousModuleS private(patternLiterals: Set[String]) extends MatchingModuleS {
  private val patterns: Set[Pattern] = patternLiterals
    .map(globToRegex)
    .map(Pattern.compile)

  override def contains(elementName: ElementNameS): Boolean =
    patterns.exists(p => p.matcher(elementName.asJavaName).matches)
}

object AnonymousModuleS {
  def make(globs: Seq[String]): Option[AnonymousModuleS] = (Try {
    Some(AnonymousModuleS(globs.toSet))
  } recover {
    case _: PatternSyntaxException => None
  }).get

  private def globToRegex(glob: String): String = {
    val stringBuilder = new StringBuilder("^")
    glob.foreach {
      case '$' => stringBuilder append "\\$"
      case '*' => stringBuilder append ".*"
      case '?' => stringBuilder append '.'
      case '.' => stringBuilder append "\\."
      case '\\' => stringBuilder append "\\\\"
      case default => stringBuilder append default
    }
    stringBuilder.toString()
  }
}