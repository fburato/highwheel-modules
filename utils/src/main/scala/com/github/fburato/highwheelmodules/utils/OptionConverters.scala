/*
 * Adapted from the Scala 2.13 library for utilisation in 2.12,
 * original source at https://github.com/scala/scala/blob/2.13.x/src/library/scala/jdk/OptionConverters.scala
 */
/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc.
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package com.github.fburato.highwheelmodules.utils

import java.util.{Optional, OptionalDouble, OptionalInt, OptionalLong}
import java.{lang => jl}
import scala.annotation.implicitNotFound

object OptionConverters {

  /** Provides conversions from Java `Optional` to Scala `Option` and specialized `Optional` types */
  implicit class RichOptional[A](private val o: java.util.Optional[A]) extends AnyVal {

    /** Convert a Java `Optional` to a Scala `Option` */
    def toScala: Option[A] = if (o.isPresent) Some(o.get) else None

    /** Convert a Java `Optional` to a Scala `Option` */
    @deprecated("Use `toScala` instead", "2.13.0")
    def asScala: Option[A] = if (o.isPresent) Some(o.get) else None

    /** Convert a generic Java `Optional` to a specialized variant */
    def toJavaPrimitive[O](implicit shape: OptionShape[A, O]): O = shape.fromJava(o)
  }

  /** Provides conversions from Scala `Option` to Java `Optional` types */
  implicit class RichOption[A](private val o: Option[A]) extends AnyVal {

    /** Convert a Scala `Option` to a generic Java `Optional` */
    def toJava: Optional[A] = o match {
      case Some(a) => Optional.ofNullable(a); case _ => Optional.empty[A]
    }

    /** Convert a Scala `Option` to a generic Java `Optional` */
    @deprecated("Use `toJava` instead", "2.13.0")
    def asJava: Optional[A] = o match {
      case Some(a) => Optional.ofNullable(a); case _ => Optional.empty[A]
    }

    /** Convert a Scala `Option` to a specialized Java `Optional` */
    def toJavaPrimitive[O](implicit shape: OptionShape[A, O]): O = shape.fromScala(o)
  }

  /** Provides conversions from `OptionalDouble` to Scala `Option` and the generic `Optional` */
  implicit class RichOptionalDouble(private val o: OptionalDouble) extends AnyVal {

    /** Convert a Java `OptionalDouble` to a Scala `Option` */
    def toScala: Option[Double] = if (o.isPresent) Some(o.getAsDouble) else None

    /** Convert a Java `OptionalDouble` to a Scala `Option` */
    @deprecated("Use `toScala` instead", "2.13.0")
    def asScala: Option[Double] = if (o.isPresent) Some(o.getAsDouble) else None

    /** Convert a Java `OptionalDouble` to a generic Java `Optional` */
    def toJavaGeneric: Optional[Double] =
      if (o.isPresent) Optional.of(o.getAsDouble) else Optional.empty[Double]
  }

  /** Provides conversions from `OptionalInt` to Scala `Option` and the generic `Optional` */
  implicit class RichOptionalInt(private val o: OptionalInt) extends AnyVal {

    /** Convert a Java `OptionalInt` to a Scala `Option` */
    def toScala: Option[Int] = if (o.isPresent) Some(o.getAsInt) else None

    /** Convert a Java `OptionalInt` to a Scala `Option` */
    @deprecated("Use `toScala` instead", "2.13.0")
    def asScala: Option[Int] = if (o.isPresent) Some(o.getAsInt) else None

    /** Convert a Java `OptionalInt` to a generic Java `Optional` */
    def toJavaGeneric: Optional[Int] =
      if (o.isPresent) Optional.of(o.getAsInt) else Optional.empty[Int]
  }

  /** Provides conversions from `OptionalLong` to Scala `Option` and the generic `Optional` */
  implicit class RichOptionalLong(private val o: OptionalLong) extends AnyVal {

    /** Convert a Java `OptionalLong` to a Scala `Option` */
    def toScala: Option[Long] = if (o.isPresent) Some(o.getAsLong) else None

    /** Convert a Java `OptionalLong` to a Scala `Option` */
    @deprecated("Use `toScala` instead", "2.13.0")
    def asScala: Option[Long] = if (o.isPresent) Some(o.getAsLong) else None

    /** Convert a Java `OptionalLong` to a generic Java `Optional` */
    def toJavaGeneric: Optional[Long] =
      if (o.isPresent) Optional.of(o.getAsLong) else Optional.empty[Long]
  }
}

/*
 * Adapted from the Scala 2.13 library for utilisation in 2.12,
 * original source at https://github.com/scala/scala/blob/2.13.x/src/library/scala/jdk/OptionShape.scala
 */
/** A type class implementing conversions from a generic Scala `Option` or Java `Optional` to
  * a specialized Java variant (for `Double`, `Int` and `Long`).
  *
  * @tparam A the primitive type wrapped in an option
  * @tparam O the specialized Java `Optional` wrapping an element of type `A`
  */
@implicitNotFound("No specialized Optional type exists for elements of type ${A}")
sealed abstract class OptionShape[A, O] {

  /** Converts from `Optional` to the specialized variant `O` */
  def fromJava(o: Optional[A]): O

  /** Converts from `Option` to the specialized variant `O` */
  def fromScala(o: Option[A]): O
}

object OptionShape {
  implicit val doubleOptionShape: OptionShape[Double, OptionalDouble] =
    new OptionShape[Double, OptionalDouble] {
      def fromJava(o: Optional[Double]): OptionalDouble =
        if (o.isPresent) OptionalDouble.of(o.get) else OptionalDouble.empty

      def fromScala(o: Option[Double]): OptionalDouble = o match {
        case Some(d) => OptionalDouble.of(d)
        case _       => OptionalDouble.empty
      }
    }
  implicit val jDoubleOptionShape: OptionShape[jl.Double, OptionalDouble] =
    doubleOptionShape.asInstanceOf[OptionShape[jl.Double, OptionalDouble]]

  implicit val intOptionShape: OptionShape[Int, OptionalInt] = new OptionShape[Int, OptionalInt] {
    def fromJava(o: Optional[Int]): OptionalInt =
      if (o.isPresent) OptionalInt.of(o.get) else OptionalInt.empty

    def fromScala(o: Option[Int]): OptionalInt = o match {
      case Some(d) => OptionalInt.of(d)
      case _       => OptionalInt.empty
    }
  }
  implicit val jIntegerOptionShape: OptionShape[jl.Integer, OptionalInt] =
    intOptionShape.asInstanceOf[OptionShape[jl.Integer, OptionalInt]]

  implicit val longOptionShape: OptionShape[Long, OptionalLong] =
    new OptionShape[Long, OptionalLong] {
      def fromJava(o: Optional[Long]): OptionalLong =
        if (o.isPresent) OptionalLong.of(o.get) else OptionalLong.empty

      def fromScala(o: Option[Long]): OptionalLong = o match {
        case Some(d) => OptionalLong.of(d)
        case _       => OptionalLong.empty
      }
    }
  implicit val jLongOptionShape: OptionShape[jl.Long, OptionalLong] =
    longOptionShape.asInstanceOf[OptionShape[jl.Long, OptionalLong]]
}
