# Basic queries

__D4S__ has a rich support of almost all DynamoDB operations. We will start with the most basic ones:
+ getItem - retrieves a single item from a table. 
+ putItem -  adds a new item or replaces existing one.
+ updateItem - updates an existing item.
+ deleteItem - removes an item from a table.
+ scan - fetches all elements in a table.
+ query - fetches all elements of a table that match some criteria using hash/range keys.

To run any query that built using D4S you must use `DynamoConnector`. Say we have a variable named `connector` of `DynamoConnector` type, 
then typical call to the database would look like this:
```scala
connector.run("query name") {
  // query itself.
}
```
Remember our example with leaderboard service? Let's implement `Ladder` interface and see how we could use one of the listed above queries
to interact with DB. If you forget how `Ladder` interface looks like, here is a quick reminder:
```scala
trait Ladder[F[_, _]] {
  def submitScore(userId: UserId, score: Score): F[QueryFailure, Unit]
  def getScores: F[QueryFailure, List[UserWithScore]]
}
```
Typical implementation of the persistence layer using D4S could look like this: 
```scala
final class D4SLadder[F[+_, +_]: Bifunctor2](connector: DynamoConnector[F], ladderTable: LadderTable) extends Ladder[F] {
  import ladderTable._

  override def getScores: F[QueryFailure, List[UserWithScore]] = {
    connector
      .run("get scores query") {
        table.scan.decodeItems[UserIdWithScoreStored].execPagedFlatten()
      }
      .leftMap(err => QueryFailure(err.message, err.cause))
      .map(_.map(_.toAPI))
  }

  override def submitScore(userId: UserId, score: Score): F[QueryFailure, Unit] = {
    connector
      .run("submit user's score") {
        table.updateItem(UserIdWithScoreStored(userId.value, score.value))
      }.leftMap(err => QueryFailure(err.message, err.cause)).void
  }
}
```
A lot of things happening here, but don't worry we'll explain everything in a bit. `D4SLadder` constructor requires two parameters: `DynamoConnector`
to run a query and `LadderTable` which is our [table definition](table-definition.md). We also require and instance of `Bifunctor2` from Izumi for `leftMap`.

## Scan and Query
In order to retrieve scores, we need to scan the whole ladder table. All queries are implemented as extension methods for `TableReferece` data type.
So to build a query you need to use `table` value from `LadderTable` as we described here:
```scala
table.scan.decodeItems[UserIdWithScoreStored].execPagedFlatten()
``` 
This query simply scans the table and decodes items (using previously defined codecs). Okay, but why we need this `.execPagedFlatten()` combinator?
We could have a huge number of records in the table that couldn't fit in one page of scan result. Using `execPagedFlatten` we create a query 
that handles pagination and flattens all pages into a single one-dimensional list of items. What if we'll change our interface and add one more method
to fetch records with users that have a score greater or equal to 42. This is how such a query could be expressed with D4S:
```scala
import d4s.implicits._
table
  .query(mainFullKey(userId))
  .withFilterExpression("score".of[Long] >= 42)
  .decodeItems[UserIdWithScoreStored]  
  .execPagedFlatten()
```
Here we use `query` operation that requires at least one table's key, we pass the user's id. You've already known about `decodeItems` and
`execPagedFlatten`, but this `withFilterExpression` combinator is something new. This combinator applies a filter on a query. 
The filter requires a `Condition` which could be build using implicit methods from `d4s.implicits` object. The `of` method specify type of the attribute
we wanna use in an expression. In our case we use score attribute and tell D4S that it has type of Long, then using method `>=` compare it with a particular value.
For more information about conditionals, please refer to [Conditionals](conditionals.md) page. 

## Put, Update and Delete
Now, let's look at `submitScore` method. The best way to put data or update it if it's already in the table using `updateItem` query.
All you need is to pass the data you want to star into `updateItem` combinator.
```scala
table.updateItem(UserIdWithScoreStored(userId.value, score.value))
```
Put operation won't differ from update too much:
```scala
table.putItem(UserIdWithScoreStored(userId.value, score.value))
```
And the last, but not least is delete operation. D4S has a `deleteItem` method that takes a table's key and removes and item.
Let's use it to remove a particular score from the ladder:
```scala
table.deleteItem(mainFullKey(userId))
```

Okay, in this section we finally discovered how to make queries with D4S and before we go to know for other operation and possibilities
that D4S provides, we would like to highlight what we've learnt so far:
- we able to setup project with D4S
- we able to define a table
- we able to perform basic operations on DynamoDB.

Good job, and see ya in the next chapter!