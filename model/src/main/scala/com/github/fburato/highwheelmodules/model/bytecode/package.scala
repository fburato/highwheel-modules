package com.github.fburato.highwheelmodules.model

package object bytecode {

  sealed trait AccessTypeS {
    def strength: Int
  }

  case object USES extends AccessTypeS {
    override def strength: Int = 1
  }

  case object COMPOSED extends AccessTypeS {
    override def strength: Int = 2
  }

  case object INHERITANCE extends AccessTypeS {
    override def strength: Int = 4
  }

  case object IMPLEMENTS extends AccessTypeS {
    override def strength: Int = 4
  }

  case object ANNOTATED extends AccessTypeS {
    override def strength: Int = 2
  }

  case object SIGNATURE extends AccessTypeS {
    override def strength: Int = 3
  }

}
