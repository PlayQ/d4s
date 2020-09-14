# Getting Started

__d4s__ is a Scala library that allows you to work with DynamoDB in a pure functional way. 
It's powered by [Izumi](https://izumi.7mind.io/latest/release/doc/index.html), uses Bifunctor IO and allows you to choose whatever effect type you want to use. 
It provides flexible and extensible DSL, supports AWS SDK v2 and has great integration with [ZIO](https://zio.dev/) and [Monix-BIO](https://bio.monix.io/) (not yet but soon).

## Dependencies

To use `d4s`, add the following line in your `build.sbt` file:

```
libraryDependencies += "net.playq" %% "d4s" %% "1.0.13"
```
The following modules are optional:

In case you want to have Circe codecs you should also add this:
```
libraryDependencies += "net.playq" %% "d4s-circe" %% "1.0.13"
```
If you want to use the metrics package from d4s you can add it like this:
```
libraryDependencies += "net.playq" %% "metrics" %% "1.0.10"
```

## A simple example
Let's imagine that we have the following interface for a game's ladders repository: 
```scala
trait Ladder[F[_, _]] {
  def submitScore(userId: UUID, score: Long): F[DynamoException, Unit]
  def getScores: F[DynamoException, List[UserWithScore]]
}
```
We need to provide table definition first, e.g. table name, hash key, range key (if exists), etc.
To do this the one should extend `TableDef` trait and pass `DynamoMeta` which is required by `TableDDL`

```scala
final class LadderTable(implicit meta: DynamoMeta) extends TableDef {
  val mainKey = DynamoKey(hashKey = DynamoField[UUID]("userId"))

  override val table: TableReference = TableReference("d4s-ladder-table", mainKey)

  override val ddl: TableDDL = TableDDL(table)
}
```
We mustn't forget  to  provide codecs for our custom type that we wanna store in the DB.
Hopefully, d4s has capabilities to automatically derive codes from user's defined types.
```scala
object LadderTable {
  final case class UserIdWithScore(userId: UUID, score: Long)
  object UserIdWithScore {
    implicit val codec: D4SCodec[UserIdWithScore] = D4SCodec.derived[UserIdWithScore]
  }
}
```
By default, d4s relies on [Magnolia](https://propensive.com/opensource/magnolia/) to derive typeclasses,
but you could also use [Circe](https://circe.github.io/circe/) to do the same. In case you wanna use `circe` just
include `d4s-circe` module as a dependency for your project.

Finally, we can write some queries!
To build a query we need `DynamoConnector` and of course our previously defined table.
`DynamoConnector` is meant to execute a query.

In order to get all scores from the ladder we need to scan the whole table. The `execPagedFlatten` combinator
handles pagination and flattens the result to one dimensional list.
```scala
connector.run("get scores query") {
  table.scan.decodeItems[UserIdWithScoreStored].execPagedFlatten()
}
```
We also wanna update our game ladder with new scores. Here is how we can easily do this:
```scala
 connector.run("submit user's score") {
  table.updateItem(UserIdWithScoreStored(userId.value, score.value))
}
```
Full implementation of `Ladder[_]`  could look like this:
```scala
final class D4SLadder[F[+_, +_]: BIO](connector: DynamoConnector[F], ladderTable: LadderTable) extends Ladder[F] {
  import ladderTable._

  override def getScores: F[DynamoException, List[UserWithScore]] = {
    connector
      .run("get scores query") {
        table.scan.decodeItems[UserIdWithScore].execPagedFlatten()
      }
  }

  override def submitScore(userId: UUID, score: Long): F[DynamoException, Unit] = {
    connector
      .run("submit user's score") {
        table.updateItem(UserIdWithScore(userId, score))
      }.void
  }
}
```

In case you wanna have a deeper look on project with d4s, you could play with it using this showcase project: [d4s-example](https://github.com/VladPodilnyk/d4s-example) 
