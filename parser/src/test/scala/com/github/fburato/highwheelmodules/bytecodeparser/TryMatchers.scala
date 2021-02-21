package com.github.fburato.highwheelmodules.bytecodeparser

import org.scalatest.matchers.{MatchResult, Matcher}

import scala.util.{Failure, Success, Try}

object TryMatchers {
  def beSuccessWith[T](successMatcher: T => Unit): Matcher[Try[T]] = {
    case left @ Failure(_) =>
      MatchResult(
        matches = false,
        s"$left was supposed to be a success",
        s"$left was not supposed to be a success"
      )
    case left @ Success(value) =>
      successMatcher(value)
      MatchResult(
        matches = true,
        s"never taken",
        s"$left matches does not cause assertion failures"
      )
  }

  def beSuccess[T]: Matcher[Try[T]] = {
    case f @ Failure(_) =>
      MatchResult(
        matches = false,
        s"$f was supposed to be a success",
        s"$f was not supposed to be a success"
      )
    case Success(_) => MatchResult(matches = true, s"never taken", "never taken")
  }

  def beFailureWith[T](failureMatcher: Throwable => Unit): Matcher[Try[T]] = {
    case left @ Success(_) =>
      MatchResult(
        matches = false,
        s"$left was supposed to be a failure",
        s"$left was not supposed to be a success"
      )
    case left @ Failure(exception) =>
      failureMatcher(exception)
      MatchResult(
        matches = true,
        s"never taken",
        s"$left matches does not cause assertion failures"
      )
  }
}
