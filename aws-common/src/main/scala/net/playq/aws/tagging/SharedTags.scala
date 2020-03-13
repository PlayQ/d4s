package net.playq.aws.tagging

object SharedTags {
  val markedForDeletion: (String, String) = {
    "marked_for_deletion" -> ""
  }
}
