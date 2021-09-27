# **D4S**  - *"Dynamo DB Database Done Scala-way"*
<p align="center">
<img width="40%" src="./vuepress/docs/.vuepress/public/D4S_logo.svg" alt="Logo"/>
</p>

ATTENTION:
==========
 ### __The repo is archived. D4S is distributed as a part of [PlayQ Toolkit](https://github.com/PlayQ/playq-tk/tree/develop/d4s).__

What is it?  ![Build](https://github.com/PlayQ/d4s/workflows/Build/badge.svg)
===========
__*D4S*__ - is a Scala library that allows you to work with DynamoDB in a pure functional way.
It's powered by [Izumi](https://izumi.7mind.io/latest/release/doc/index.html), uses Bifunctor IO and allows you to choose whatever effect type you want to use. It provides flexible and extensible DSL, supports AWS SDK v2 and has great integration with [ZIO](https://zio.dev/).

include the following components:

1. _d4s_ – core package, the lib itself.
2. _d4s-circe_ – provides circe codecs to encode the data.
3. _d4s-test_ - provides test environment and docker containers via DIstage TestKit.
4. _metrics_ - a small yet convenient package for metrics aggregation.
5. _aws-common_ - tagging and namespaces

Please proceed to the [microsite](https://playq.github.io/d4s/) for more information.   
