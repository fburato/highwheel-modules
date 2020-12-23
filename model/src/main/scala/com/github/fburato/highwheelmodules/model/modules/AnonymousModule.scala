package com.github.fburato.highwheelmodules.model.modules

import com.github.fburato.highwheelmodules.model.bytecode.ElementName
import com.github.fburato.highwheelmodules.model.modules.AnonymousModule.globToRegex

import java.util.regex.{Pattern, PatternSyntaxException}
import scala.util.Try

case class AnonymousModule private(patternLiterals: Set[String]) extends MatchingModule {
  private val patterns: Set[Pattern] = patternLiterals
    .map(globToRegex)
    .map(Pattern.compile)

  override def contains(elementName: ElementName): Boolean =
    patterns.exists(p => p.matcher(elementName.asJavaName).matches)
}

object AnonymousModule {
  def make(globs: Seq[String]): Option[AnonymousModule] = (Try {
    Some(AnonymousModule(globs.toSet))
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