package com.github.fburato.highwheelmodules.model

package object bytecode {

  sealed trait AccessType {
    def strength: Int
  }

  case object USES extends AccessType {
    override def strength: Int = 1
  }

  case object COMPOSED extends AccessType {
    override def strength: Int = 2
  }

  case object INHERITANCE extends AccessType {
    override def strength: Int = 4
  }

  case object IMPLEMENTS extends AccessType {
    override def strength: Int = 4
  }

  case object ANNOTATED extends AccessType {
    override def strength: Int = 2
  }

  case object SIGNATURE extends AccessType {
    override def strength: Int = 3
  }

}
