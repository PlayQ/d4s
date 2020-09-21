# Table definition

## App structure
Our `LeaderBoardService` will have the following functionality:
- store ladder (user's id with score)
- store profile (user's name and profile description)
- calculate rank based on user's score (just for fun :smiley:)

Here is how interfaces for our tables will look like:
```scala
trait Ladder[F[_, _]] {
  def submitScore(userId: UserId, score: Score): F[QueryFailure, Unit]
  def getScores: F[QueryFailure, List[UserWithScore]]
}

trait Profiles[F[_, _]] {
  def setProfile(userId: UserId, profile: UserProfile): F[QueryFailure, Unit]
  def getProfile(userId: UserId): F[QueryFailure, Option[UserProfile]]
}

trait Ranks[F[_, _]] {
  def getRank(userId: UserId): F[QueryFailure, Option[RankedProfile]]
}
```
Here is how `UserId`, `Score` and `UserProfile definitions look like
```scala
final case class UserId(value: UUID) extends AnyVal
final case class Score(value: Long) extends AnyVal
final case class UserProfile(userName: String, description: String)
```
`RankedProfile` contains user's name and description with score and rank
```scala
final case class RankedProfile(
  name: String,
  description: String,
  rank: Int,
  score: Score,
)
```
We also want to define a domain failure 
```scala
final case class QueryFailure(queryName: String, cause: Throwable)
  extends RuntimeException(
    s"""Query "$queryName" failed with ${cause.getMessage}""",
    cause,
  )
```

## Define a table
We will skip dummy implementation for our repo layer to avoid information noise, but you could find complete code [here](https://github.com/VladPodilnyk/d4s-example/tree/0ef2a95d12d9fb26206bf6ba25b2a5c67eba640d/src/main/scala/leaderboard/repo).

### Ladder table
Let's define a table for ladder first.
```scala
final class LadderTable(implicit meta: DynamoMeta) extends TableDef {
  private[this] val mainKey = DynamoKey(hashKey = DynamoField[UUID]("userId"))

  override val table: TableReference = TableReference("d4s-ladder-table", mainKey)

  override val ddl: TableDDL = TableDDL(table)

  def mainFullKey(userId: UserId): Map[String, AttributeValue] = {
    mainKey.bind(userId.value)
  }
}
```
To define a table you must extend `TableDef` trait which has `table` and `ddl` values. We have a SUPER-simple table
with `hash key` only. `DynamoFiled` is used to describe type of the key. Here we described a helper function `mainFullKey`
to access key indirectly, which is not necessary thing to do. Also, we must define codecs that convert data types we wanna store in DB
to AWS format. Hopefully, D4S has capabilities to automatically derive codes from user's defined types. This could be made with
`D4SCodec.derived` macros that successfully derives an encoder and decoder for you custom data type.
```scala
object LadderTable {
  final case class UserIdWithScoreStored(userId: UUID, score: Long){
    def toAPI: UserWithScore = UserWithScore(UserId(userId), Score(score))
  }
  object UserIdWithScoreStored {
    implicit val codec: D4SCodec[UserIdWithScoreStored] = D4SCodec.derived[UserIdWithScoreStored]
  }
}
```
By default, D4S relies on [Magnolia](https://propensive.com/opensource/magnolia/) to derive typeclasses,
but you could also use [Circe](https://circe.github.io/circe/) to do the same. In case you wanna use `circe` just
include `d4s-circe` module as a dependency for your project.

### Profile table
We repeat the same procedure with `Profile` table.
```scala
final class ProfilesTable(implicit meta: DynamoMeta) extends TableDef {
  private[this] val mainKey = DynamoKey(hashKey = DynamoField[UUID]("userId"))

  override val table: TableReference = TableReference("d4s-profile-table", mainKey)

  override val ddl: TableDDL = TableDDL(table)

  def mainFullKey(userId: UserId): Map[String, AttributeValue] = {
    mainKey.bind(userId.value)
  }
}

object ProfilesTable {
  final case class UserProfileWithIdStored(userId: UUID, userName: String, description: String) {
    def toAPI: UserProfileWithId = UserProfileWithId(UserId(userId), UserProfile(userName, description))
  }
  object UserProfileWithIdStored {
    implicit val codec: D4SCodec[UserProfileWithIdStored] = D4SCodec.derived[UserProfileWithIdStored]
  }
}
```
Now, we are ready to make some queries!!!
