package routes

import db.Database
import services.AggregateService

object ProfileRoutes extends BaseRoutes:

  @cask.get("/api/profile")
  def getProfile(request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        AggregateService.getProfile(userId) match
          case None =>
            err("NOT_FOUND", "Profile not found", 404)
          case Some(p) =>
            ok(profileToJson(p))
      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  @cask.put("/api/profile")
  def updateProfile(request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val body          = ujson.read(request.text())
        val bio           = body.obj.get("bio").flatMap(v => if v.isNull then None else Some(v.str))
        val goal          = body("goal").str
        val targetKcal    = body("targetKcal").num.toInt
        val targetProtein = body("targetProteinG").num.toInt
        val targetCarbs   = body("targetCarbsG").num.toInt
        val targetFat     = body("targetFatG").num.toInt
        val targetFiber        = body("targetFiberG").num.toInt
        val targetSaturatedFat = body("targetSaturatedFatG").num.toInt
        val targetWater        = body("targetWaterL").num
        val baseWeight    = body.obj.get("baseWeightKg").flatMap(v => if v.isNull then None else Some(v.num))
        val goalWeight    = body.obj.get("goalWeightKg").flatMap(v => if v.isNull then None else Some(v.num))

        val profile = Database.withConnection { conn =>
          val st = conn.prepareStatement(
            """INSERT INTO user_profile
              |  (user_id, bio, goal, target_kcal, target_protein_g, target_carbs_g,
              |   target_fat_g, target_fiber_g, target_saturated_fat_g,
              |   target_water_l, base_weight_kg, goal_weight_kg)
              |VALUES (?::uuid, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
              |ON CONFLICT (user_id) DO UPDATE SET
              |  bio                   = EXCLUDED.bio,
              |  goal                  = EXCLUDED.goal,
              |  target_kcal           = EXCLUDED.target_kcal,
              |  target_protein_g      = EXCLUDED.target_protein_g,
              |  target_carbs_g        = EXCLUDED.target_carbs_g,
              |  target_fat_g          = EXCLUDED.target_fat_g,
              |  target_fiber_g        = EXCLUDED.target_fiber_g,
              |  target_saturated_fat_g = EXCLUDED.target_saturated_fat_g,
              |  target_water_l        = EXCLUDED.target_water_l,
              |  base_weight_kg        = EXCLUDED.base_weight_kg,
              |  goal_weight_kg        = EXCLUDED.goal_weight_kg,
              |  updated_at            = now()
              |RETURNING id, user_id, bio, goal, target_kcal, target_protein_g, target_carbs_g,
              |          target_fat_g, target_fiber_g, target_water_l, base_weight_kg,
              |          goal_weight_kg, updated_at""".stripMargin
          )
          st.setString(1, userId)
          bio match
            case Some(b) => st.setString(2, b)
            case None    => st.setNull(2, java.sql.Types.VARCHAR)
          st.setString(3, goal)
          st.setInt(4, targetKcal)
          st.setInt(5, targetProtein)
          st.setInt(6, targetCarbs)
          st.setInt(7, targetFat)
          st.setInt(8, targetFiber)
          st.setInt(9, targetSaturatedFat)
          st.setDouble(10, targetWater)
          baseWeight match
            case Some(w) => st.setDouble(11, w)
            case None    => st.setNull(11, java.sql.Types.NUMERIC)
          goalWeight match
            case Some(w) => st.setDouble(12, w)
            case None    => st.setNull(12, java.sql.Types.NUMERIC)
          val rs = st.executeQuery()
          rs.next()
          // Re-use AggregateService's profileFromRs by reading back from DB
          AggregateService.getProfile(userId).get
        }

        ok(profileToJson(profile))

      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  private def profileToJson(p: models.UserProfile): ujson.Obj =
    ujson.Obj(
      "id"             -> p.id,
      "userId"         -> p.userId,
      "bio"            -> p.bio.map(ujson.Str.apply).getOrElse(ujson.Null),
      "goal"           -> p.goal,
      "targetKcal"     -> p.targetKcal,
      "targetProteinG" -> p.targetProteinG,
      "targetCarbsG"   -> p.targetCarbsG,
      "targetFatG"     -> p.targetFatG,
      "targetFiberG"        -> p.targetFiberG,
      "targetSaturatedFatG" -> p.targetSaturatedFatG,
      "targetWaterL"        -> p.targetWaterL,
      "baseWeightKg"   -> p.baseWeightKg.map(ujson.Num.apply).getOrElse(ujson.Null),
      "goalWeightKg"   -> p.goalWeightKg.map(ujson.Num.apply).getOrElse(ujson.Null),
      "updatedAt"      -> p.updatedAt,
    )

  initialize()
