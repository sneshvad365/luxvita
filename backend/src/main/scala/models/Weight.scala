package models

import upickle.default.*

case class WeightLog(
  id       : String,
  userId   : String,
  loggedAt : String,
  weightKg : Double,
) derives ReadWriter

case class WeightTrend(
  entries       : List[WeightLog],
  sevenDayAvg   : Option[Double],
  paceKgPerWeek : Option[Double],
  etaDays       : Option[Int],
) derives ReadWriter
