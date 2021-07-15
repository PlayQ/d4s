# Batched operations

Sometimes we need to work with a large amount of data. Using standard operations like `put`/`update`/`delete` 
to modify data in a database could affect system performance. Fortunately, AWS team implement batch operations
for DynamoDB.

So, in case we want to `delete` records for several users from leaderboard table, we could do this by typing
the following:
```scala
val usersToDelete: List[UserId] = ...
connector.run("batch-delete") {
  table.deleteItemBatch(usersToDelete.map(mainFullKey))
}
```
_Important note_: `deleteItemBatch` operation requires implicit `D4SEncoder` that converts user's concrete type
to `Map[String, AttributeValue]`. Here we take a slightly different way. Leaderbord table has simple key that contains
user's UUID only. In table definition we described a helper function `mainFullKey` that encodes `UserId` for use:
```scala
def mainFullKey(value: UserId): Map[String, AttributeValue]  = {
  mainKey.bind(userId.value)
}
```
Alternatively, you can handle this by extending abstract class WithD4S which provides codecs for you:
```scala
final case class UserId(id: UUID) extends AnyVal
object UserId extends WithD4S[UserId]

```

Now, let's say we want to get scores of players that are on the same team. We can use `getItemBatch` combinator
to achieve this:
```scala
val team: List[UserId] = ...
connector.run("get-team-members-scores") {
  table.getItemBatch(usersToDelete.map(mainFullKey))
}
```
That's it!!! Basically, the code is the same in both cases (get/delete), the only difference is type of operation we want to
execute. Same thing applies for `put` operation. We can easily insert a new data with `putItemBatch` combinator.
Be aware, that for this operation you need to specify all attributes e.g.
```scala
final case class UserWithScore(id: UserId, score: Long)
object UserWithScore extends WithD4S[UserWithScore]

val data: List[UserWithScore] = ...
connector.run("write-batch") {
  table.putItemBatch(data)
}
```
Note, that all `d4s` batched operations handles pagination by default, so you can sleep well at night :).
One thing has been uncovered by us so far. It's combinators that built on top of DynamoDB operations.
Sometimes we perform queries that could be described using several DynamoDB operations sequentially.
In our production experience we found `query-delete` patter very common, so we provide convenient combinator 
`queryDeleteBatch` for that. Imagine that we want to deliver a prize for players that won our game's tournament or 
finished a hard level with a high score.
Obviously, we need to have a separate table for that purpose. In that case table's key will have hash and range part, 
user's UUID and prize UUID respectively. Let's say the user collect his prizes thus we have no need to store them.
Here is how we can remove all prized granted to a specific user using `queryDeleteBatch`:
```scala
def deletePackages(user: UserId) = {
  connector.run("remove-prizes) {
    table
      .queryDeleteBatch
      .withKey(prizesTable.key.bind(user.id))
  }
}
```
The query above fetches all recs that has hash key equal to specified id and then in parallel fashion deletes data chunks
using Dynamo's batch write operation. 
Note: we omit table definition here because we've already known how to do this. `prizesTable` has a `key` of type `DynamoKey`.
`DynamoKey` type has bind method that helps us convert concrete type to `Map[String, AttributeValue]`.

Okay, that all. Now you know how to perform batched operation with d4s and ready to use it on the battleground.
In the next section we will learn more about conditionals.
