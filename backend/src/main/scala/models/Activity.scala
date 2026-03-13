package models

import upickle.default.*

case class ActivityLog(
  id       : String,
  userId   : String,
  loggedAt : String,
  entry    : String,
  parsed   : Option[ujson.Value] = None,
) derives ReadWriter

case class ParsedActivity(
  `type`      : String,                 // gym | run | walk | sport | rest | ...
  durationMin : Option[Int],
  intensity   : Option[String],         // low | moderate | high
  steps       : Option[Int],
  mood        : Option[String],
  notes       : Option[String],
) derives ReadWriter
