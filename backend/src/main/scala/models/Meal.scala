package models

import upickle.default.*

case class Meal(
  id          : String,
  userId      : String,
  loggedAt    : String,
  description : Option[String],
  hasPhoto    : Boolean,
  kcal        : Option[Int],
  proteinG    : Option[Double],
  carbsG      : Option[Double],
  fatG        : Option[Double],
  fiberG      : Option[Double],
  rawEstimate : Option[ujson.Value] = None,
) derives ReadWriter

case class MacroEstimate(
  kcal      : Int,
  proteinG  : Double,
  carbsG    : Double,
  fatG      : Double,
  fiberG    : Double,
  description: String,
) derives ReadWriter

case class TodayTotals(
  kcal    : Int,
  proteinG: Double,
  carbsG  : Double,
  fatG    : Double,
  fiberG  : Double,
  meals   : List[Meal],
) derives ReadWriter
