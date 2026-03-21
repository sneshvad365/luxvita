package models

import upickle.default.*

/** Shared macro targets / running totals used across multiple response types. */
case class Macros(
  kcal         : Int,
  proteinG     : Double,
  carbsG       : Double,
  fatG         : Double,
  saturatedFatG: Double,
  fiberG       : Double,
) derives ReadWriter
