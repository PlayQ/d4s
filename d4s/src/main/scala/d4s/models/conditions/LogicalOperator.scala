package d4s.models.conditions

trait LogicalOperator {
  def asString: String
}

object LogicalOperator {
  case object < extends LogicalOperator {
    override val asString: String = "<"
  }
  case object == extends LogicalOperator {
    override val asString: String = "="
  }
  case object <> extends LogicalOperator {
    override val asString: String = "<>"
  }
  case object > extends LogicalOperator {
    override val asString: String = ">"
  }
  case object <= extends LogicalOperator {
    override val asString: String = "<="
  }
  case object >= extends LogicalOperator {
    override val asString: String = ">="
  }
}
