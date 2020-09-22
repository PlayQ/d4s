# Setup

We've already mentioned that `d4s` is powered by `Izumi`. In fact, `d4s` is the first library in Izumi's ecosystem, thus `d4s` heavily relies on it. 
Further, we'll talk about Izumi's components that allows `d4s` to archive excellence.

Now, let's talk about the preparation stuff we need to do. There are many ways to handle dependencies, but generally, they can fall in either of the two categories: 
with DI or without DI. `PlayQ`'s team recommends to use DiStage as a dependency injection
framework that handles all dependencies for you and even more. _Important note_: in case you don't like DI or don't want to use it
you can [go ahead and look](#managing-dependencies-manually) what dependencies you need to have to start work with DynamoDB.

## Managing dependencies with DIStage :rocket:
[DIStage](https://izumi.7mind.io/distage) allows you to manage dependencies with ease, so you can reduce
boilerplate code that is needed to construct objects in a program and spend more time developing crucial things.
With [DIStage](https://izumi.7mind.io/distage) your will embrace a [role-based](https://izumi.7mind.io/latest/release/doc/distage/distage-framework.html#roles) approach to structure an application
which simplifies deployments. In case those words does not mean anything to you, don't worry, we will gently guide you
and teach you everything you need to know. So, to start with DIStage we need to describe a role (could be many), 
create a plugin with our dependencies. Let's describe role first.
```scala
final class LeaderboardServiceRole[F[+_, +_]: ConcurrentThrowable: TTimer](httpApi: HttpApi[F]) extends RoleService[F[Throwable, ?]] {
  override def start(roleParameters: RawEntrypointParams, freeArgs: Vector[String]): DIResource.DIResourceBase[F[Throwable, ?], Unit] = {
    for {
      _ <- DIResource.fromCats {
        BlazeServerBuilder
          .apply[F[Throwable, ?]](global)
          .withHttpApp(httpApi.routes.orNotFound)
          .bindLocal(8080)
          .resource
      }
    } yield ()
  }
}

object MainProd extends MainBase(Activation(CustomAxis -> CustomAxis.Prod))
object MainDummy extends MainBase(Activation(CustomAxis -> CustomAxis.Dummy))

object LeaderboardServiceRole extends RoleDescriptor {
  val id = "leaderboard"
}
```
Basically, to describe a role we need to extend `RoleService` and describe desired logic inside `start` method. Also, we need to define
`RoleDescriptor` which is used by DIStage to find a role. The logic inside `RoleService`'s start method is pretty simple, we just spin a server on a localhost and attach a http app, that we'll describe further.
The interesting thing here is that we use `DIResource` that safely acquire/release resources. You can also spot we use [activation axis](https://izumi.7mind.io/latest/release/doc/distage/basics.html#activation-axis) from DIStage.
We need such an axis to switch between different environments easily, in our case between dummy and prod implementation of repo layer. The full code with activation axis definition, `MainBase` and role 
you can find [here](https://github.com/VladPodilnyk/d4s-example/blob/8b74f576b9a4f9eeff0ac86dc99b2f3b3fbaa636/src/main/scala/leaderboard/LadderServiceRole.scala).
Notice, we structure our application using [TF encoding](https://izumi.7mind.io/latest/release/doc/distage/basics.html#tagless-final-style), hence `LeaderboardServiceRole` takes `F[+_, +_]` as type parameter
with several implicits. `ConcurrentThrowable` and `TTimer` are adapters for cats-effect's typeclasses that expect effect with two type "holes" as type parameter.  

Okay, now we need to define a plugin. Fortunately, D4S provides a DIStage module that you can use in your project.  
```scala
object LeaderboardPlugin extends PluginDef {
  include(AwsTagsModule)
  include(D4SModule[IO])
}
```
`D4SModule` creates everything you need to work with D4S even healthcheck. We also add `AwsTagsModule` that could be quite
helpful in case we want to tag our AWS tables.

__TODO__: plugin for effects

Huurraayyy, we've finished setting up our project, now we are ready to move further. Before we move to the next session, 
we would like to list what we achive so far:
- our app safely acquire/release resources
- we have a healthcheck for our DynamoDB
- we don't need to manually create tables (that is handled by D4S).