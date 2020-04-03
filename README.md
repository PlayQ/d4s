# **d4s**  - *"Dynamo DB Database Done Scala-way"*
<p align="center">
<img width="40%" src="./docs/resourses/D4S_logo.svg" alt="Logo"/>
</p>

What is it?  ![Build](https://github.com/PlayQ/d4s/workflows/Build/badge.svg)
===========
__*d4s*__ - is a Scala library that allows you to work with DynamoDB in a pure functional way.
It's powered by [Izumi](https://izumi.7mind.io/latest/release/doc/index.html), uses Bifunctor IO and allows you to choose whatever effect type you want to use. It provides flexible and extensible DSL, supports AWS SDK v2 and has great integration with [ZIO](https://zio.dev/).

include the following components:

1. d4s – core package, the lib itself.
2. d4s-circe – provides circe codecs to encode the data.
3. d4s-test - provides test enviroment and docker containers via DIstage TestKit.
4. metrics - a small yet convinient package for metrics aggregation.
5. aws-common - tagging and namespaces
   