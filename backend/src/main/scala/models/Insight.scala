package models

import upickle.default.*

case class Insight(
  insight : String,
  `type`  : String, // protein | timing | fiber | hydration | recovery | weight
) derives ReadWriter
