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
  photoData   : Option[String]      = None,
) derives ReadWriter

case class BreakdownItem(
  item    : String,
  kcal    : Int,
  proteinG: Double,
  carbsG  : Double,
  fatG    : Double,
  fiberG  : Double,
) derives ReadWriter

case class MacroEstimate(
  kcal       : Int,
  proteinG   : Double,
  carbsG     : Double,
  fatG       : Double,
  fiberG     : Double,
  description: String,
  waterMl    : Option[Int]           = None,
  breakdown  : List[BreakdownItem]   = Nil,
) derives ReadWriter

case class TodayTotals(
  kcal    : Int,
  proteinG: Double,
  carbsG  : Double,
  fatG    : Double,
  fiberG  : Double,
  meals   : List[Meal],
) derives ReadWriter
