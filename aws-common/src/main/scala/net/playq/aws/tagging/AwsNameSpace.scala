package net.playq.aws.tagging

import java.util.UUID

final case class AwsNameSpace(namespace: Option[String], tags: TagsConfig) {
  override def toString: String = namespace.filter(_.nonEmpty).fold("")(ns => s"$ns-")
}

object AwsNameSpace {
  def newTestNameSpace(randomString: String = UUID.randomUUID().toString.take(13)): AwsNameSpace = {
    AwsNameSpace(Some(s"test-$randomString"), TagsConfig("test", "test"))
  }
}
