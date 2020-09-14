# Basic queries

__d4s__ has a rich support of almost all DynamoDB operations. Anyway, lets begin with the most basic ones:
+ getItem - retrieves a single item from a table. 
+ putItem -  adds a new item or replaces existing one.
+ updateItem - updates an existing item.
+ deleteItem - removes an item from a table.
+ scan - fetches all elements in a table.
+ query - fetches all elements of a table that match some criteria using hash/range keys.

We should cover one important thing before we move to queries. 
In order to run any query we must have a `DynamoConnector` instance (maybe you noticed that
during a [getting started section](README.md)). So, basically, we have the  following pattern to run 
any `d4s` query:
```scala
// connector has type of DynamoConnector
connector.run("query-name") {
  // place your query here...
}
```

???? For sake of simplicity and low information noise, in examples we'll omit these two lines that are related to `DynamoConnector`.
We'll also assume the following table definition of the table:
```scala
final class LadderTable(implicit meta: DynamoMeta) extends TableDef {
  val mainKey = DynamoKey(hashKey = DynamoField[UUID]("userId"))

  override val table: TableReference = TableReference("d4s-ladder-table", mainKey)

  override val ddl: TableDDL = TableDDL(table)
```
## Put and Get
```scala
def saveItem(ladderTable: LadderTable, id: UUID, score: Long) = {
  import ladderTable._
  table.putItem(mainKey.bind(id)).withItem(score).void
}
```


## Update

## Delete

## Scan and Query

 
