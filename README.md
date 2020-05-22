# **d4s**  - *"Dynamo DB Database Done Scala-way"*
<p align="center">
<img width="40%" src="./resources/D4S_logo.svg" alt="Logo"/>
</p>

What is it?  ![Build](https://github.com/PlayQ/d4s/workflows/Build/badge.svg)
===========
__*d4s*__ - is a Scala library that allows you to work with DynamoDB in a pure functional way.
It's powered by [Izumi](https://izumi.7mind.io/latest/release/doc/index.html), uses Bifunctor IO and allows you to choose whatever effect type you want to use. It provides flexible and extensible DSL, supports AWS SDK v2 and has great integration with [ZIO](https://zio.dev/).

include the following components:

1. _d4s_ – core package, the lib itself.
2. _d4s-circe_ – provides circe codecs to encode the data.
3. _d4s-test_ - provides test environment and docker containers via DIstage TestKit.
4. _metrics_ - a small yet convenient package for metrics aggregation.
5. _aws-common_ - tagging and namespaces

Quickstart
===========
The latest version is `1.0.6`. To use it just add the following line to your `build.sbt` file:
```
// available for 2.12 and 2.13
libraryDependencies += "net.playq" %% "d4s" %% "1.0.6"
```
In case you want to have Circe codecs you should also add this:
```
// available for 2.12 and 2.13
libraryDependencies += "net.playq" %% "d4s-circe" %% "1.0.6"
```
If you want to use only the metrics package from `d4s` you can add it like this:
```
// available for 2.12 and 2.13
libraryDependencies += "net.playq" %% "metrics" %% "1.0.6"
```

Example project:

- [d4s-example](https://github.com/VladPodilnyk/d4s-example)

Materials and docs (microsite in work):
- [ScalaUA presentation](resources/presentation/scalaua.pdf)
   