package models

import upickle.default.*

case class User(
  id           : String,
  email        : String,
  passwordHash : String,
  createdAt    : String,
) derives ReadWriter

case class UserProfile(
  id            : String,
  userId        : String,
  bio           : Option[String],
  goal          : String,          // fat_loss | muscle_gain | maintenance
  targetKcal    : Int,
  targetProteinG: Int,
  targetCarbsG  : Int,
  targetFatG    : Int,
  targetFiberG  : Int,
  targetSaturatedFatG: Int = 20,
  targetWaterL  : Double,
  baseWeightKg  : Option[Double],
  goalWeightKg  : Option[Double],
  updatedAt     : String,
) derives ReadWriter
