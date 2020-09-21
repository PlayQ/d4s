# Persistence implementation

## Ladder persistence
Okay, now we are fully equipped to  implement `Ladder[F[_, _]]` and `Profile[F[_, _]]` interfaces.
Below you can see how the possible implementation of `Ladder[F[_, _]]` could look like.
```scala
final class D4SLadder[F[+_, +_]: BIOBifunctor](connector: DynamoConnector[F], ladderTable: LadderTable) extends Ladder[F] {
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
`D4SLadder` class requires `DynamoConnector` and our previously defined table. `DynamoConnecor` plays a significant role here, because
it actually runs a query. Using D4S you can encounter the following pattern to run a query:
```scala
connector.run("query name") {
  // query itself
}
```
In order to retrieve scores we need to scan whole ladder table. All queries are implemented as extension methods for `TableReferece` data type.
So to build a query you need to use `table` value from `LadderTable` like we described here:
```scala
table.scan.decodeItems[UserIdWithScoreStored].execPagedFlatten()
``` 
This query simply scans the table and decode items (using previously defined codecs). Okay, but why we need this `.execPagedFlatten()` combinator?
We could have a huge number of records in the table that couldn't fit in one page of scan result. Using `execPagedFlatten` we create a query 
that handles pagination and flatten all pages into a single one dimensional list of items.

Now, lets look at `submitScore` method. The best way to put data or update it if it's already in the table using `updateItem` query. 
```scala
table.updateItem(UserIdWithScoreStored(userId.value, score.value))
```
One thing, that we didn't cover here is this `BIOBifunctor` implicit. We require that `F[_, _]` has `BIOBifunctor` instance to use
functions like `leftMap`.

## Profile persistence
The `Profile` persistence is pretty much similar to `D4SLadder`
```scala
final class D4SProfiles[F[+_, +_]: BIOBifunctor](
  connector: DynamoConnector[F],
  profilesTable: ProfilesTable
) extends Profiles[F] {

  import profilesTable._

  override def getProfile(userId: UserId): F[QueryFailure, Option[UserProfile]] = {
    connector
      .run("get-profile") {
        table
          .getItem(mainFullKey(userId))
          .decodeItem[UserProfile]
      }.leftMap(err => QueryFailure(err.message, err.cause))
  }

  override def setProfile(userId: UserId, profile: UserProfile): F[QueryFailure, Unit] = {
    connector
      .run("set-profile") {
        table.updateItem(UserProfileWithIdStored(userId.value, profile.userName, profile.description)).void
      }.leftMap(err => QueryFailure(err.message, err.cause))
  }
}
```
The only new thing here is `getItem` query:
```scala
table.getItem(mainFullKey(userId)).decodeItem[UserProfile]
```
Here we use our helper function `mainFullKey` that return `Map[String, AttributeValue]` and is used to find an item. 
Notice that we use `decodeItem` combinator to decode a single item. Contrary in `D4SLadder` we used `decodeItems` to decode several items at once.