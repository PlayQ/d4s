# Getting started

## Overview
+ __D4S__ is a Scala library to work with DynamoDB.
+ __D4S__ is powered by [Izumi](https://izumi.7mind.io/latest/release/doc/index.html)
+ __D4S__ uses Bifunctor IO and allows you to choose whatever effect type you want to use.
+ __D4S__ provides flexible and extensible DSL 
+ __D4S__ supports AWS SDK v2
+ __D4S__ has great integration with [ZIO](https://zio.dev/) and [Monix-BIO](https://bio.monix.io/) (not yet but soon).

## Dependencies

To use `D4S`, add the following line in your `build.sbt` file:
```
libraryDependencies += "net.playq" %% "d4s" %% "1.0.13"
```

The following modules are optional:<br/>
In case you want to have Circe codecs you should also add this:
```
libraryDependencies += "net.playq" %% "d4s-circe" %% "1.0.13"
```
If you want to use the metrics package from d4s you can add it like this:
```
libraryDependencies += "net.playq" %% "metrics" %% "1.0.13"
```

## How to learn

#### Prerequisites
+ We assume you are comfortable with Amazon's DynamoDB.
+ We assume you are comfortable with functional programming concepts, including `bifunctor IO` and `tagless-final encoding`.
  Prior experience with [Izumi](https://izumi.7mind.io/latest/release/doc/index.html) or libraries like [ZIO](https://zio.dev/), [cats](https://typelevel.org/cats/), [cats-effect](https://typelevel.org/cats-effect/),
  [FS2](https://fs2.io/) would be helpful.

#### Learning resources
+ Try our [tutorial](tutorial.md)

Other resources:
+ Check out a [talk](https://www.youtube.com/watch?v=SGlQhN8CMIs&t=6s) about `d4s` on _ScalaUA2020_

## How to contribute
+ If you find a bug, open an issue (or fix it and open a PR) at our GitHub Repository.
+ If you want to make a larger contribution please open an issue first, so we can discuss.
