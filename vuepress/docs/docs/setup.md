# Setup

We've already mentioned that `D4S` is powered by `Izumi`. In fact, `D4S` is the first library in Izumi's ecosystem, thus `D4S` heavily relies on it. 
Further, we'll talk about Izumi's components that allow `D4S` to archive excellence.

Now, let's talk about the preparation stuff we need to do. There are many ways to handle dependencies, but generally, they fall in either of the two categories: 
with DI or without DI. `PlayQ`'s team recommends using Izumi's DIStage as a dependency injection framework that handles all dependencies for you and even more.
Since D4S intended to use within Izumi's ecosystem we will explain how to use DIStage to manage your dependencies. Anyway, it could be used without DI framework, 
but this setup we will try to describe in observable future.

## Managing dependencies with DIStage :rocket:
[DIStage](https://izumi.7mind.io/distage) allows you to manage dependencies with ease, so you can reduce
boilerplate code that is needed to construct objects in a program and spend more time developing crucial things.
With DIStage your will embrace a [role-based](https://izumi.7mind.io/latest/release/doc/distage/distage-framework.html#roles) approach to structure an application
which simplifies deployments. In case those words do not mean anything to you, don't worry, we will gently guide you
and teach you everything you need to know. So, to start with DIStage we need to describe a role (could be many), create a plugin with our dependencies. Let's describe a role first.
```scala
final class TestRole[F[+_, +_]] extends RoleService[F[Throwable, ?]] {
  override def start(roleParameters: RawEntrypointParams, freeArgs: Vector[String]): DIResource.DIResourceBase[F[Throwable, ?], Unit] = {
   // define your logic here...
  }
}

object MainProd extends MainBase(Activation(CustomAxis -> CustomAxis.Prod))
object MainDummy extends MainBase(Activation(CustomAxis -> CustomAxis.Dummy))

object TestRole extends RoleDescriptor {
  val id = "test-role"
}
```
Basically, to define a role we need to extend `RoleService` and describe desired logic inside `start` method. Also, we need to define `RoleDescriptor` which is used by DIStage to find a role. 
The interesting thing here is that we use `DIResource` that safely acquire/release resources. You can also spot we use [activation axis](https://izumi.7mind.io/latest/release/doc/distage/basics.html#activation-axis) from DIStage.
We need such an axis to switch between different environments easily, in our case between the dummy and prod implementation of repo layer. The full code for role definition with activation axis you could find in our 
example project here [here](https://github.com/VladPodilnyk/d4s-example/blob/8b74f576b9a4f9eeff0ac86dc99b2f3b3fbaa636/src/main/scala/leaderboard/LadderServiceRole.scala). 

Okay, now we need to define a plugin. Fortunately, D4S provides a DIStage module that you can use in your project.  
```scala
object LeaderboardPlugin extends PluginDef {
  include(AwsTagsModule)
  include(D4SModule[IO])
}
```
`D4SModule` creates everything you need to work with D4S even health check. We also add `AwsTagsModule` that could be quite
helpful in case we want to tag our AWS tables.

Finally, you have to add a plugin for effect type that will be used to interpret blueprints. Here is how plugin with ZIO could look like:
```scala
object ZIOPlugin extends PluginDef {
  include(ZIODIEffectModule)

  addImplicit[Bracket[Task, Throwable]]
  addImplicit[Async[Task]]
  addImplicit[ContextShiftThrowable[IO]]
  addImplicit[AsyncThrowable[IO]]
  addImplicit[ContextShift[Task]]
  make[TTimer[IO]].from(TTimer[IO])

  make[ConcurrentThrowable[IO]].from {
    implicit r: Runtime[Any] =>
      implicitly[ConcurrentEffect[IO[Throwable, ?]]]
  }

  make[Blocker].from {
    pool: ThreadPoolExecutor @Id("zio.io") =>
      Blocker.liftExecutionContext(ExecutionContext.fromExecutorService(pool))
  }

  make[LogIO2[IO]].from(LogIO2.fromLogger[IO] _)
}
```

Huurraayyy, we've finished setting up our project, now we are ready to move further. Before we move to the next session, 
we would like to list what we achieved so far:
- our app safely acquire/release resources
- we have a healthcheck for our DynamoDB
- we don't need to manually create tables (that is handled by D4S).