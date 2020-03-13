package d4s.models.table

trait TableDef {
  val table: TableReference
  val ddl: TableDDL
}
