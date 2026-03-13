package routes

import db.Database
import models.Macros
import services.{AggregateService, ClaudeService}
import upickle.default.*

object MealRoutes extends BaseRoutes:

  @cask.post("/api/meals")
  def logMeal(request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val body        = ujson.read(request.text())
        val description = body.obj.get("description").flatMap(v => if v.isNull then None else Some(v.str)).filter(_.nonEmpty)
        val photo       = body.obj.get("photo").flatMap(v => if v.isNull then None else Some(v.str)).filter(_.nonEmpty)

        if description.isEmpty && photo.isEmpty then
          err("INVALID_INPUT", "At least one of description or photo is required", 400)
        else
          val profile    = AggregateService.getProfileOrDefault(userId)
          val targets    = Macros(
            kcal     = profile.targetKcal,
            proteinG = profile.targetProteinG.toDouble,
            carbsG   = profile.targetCarbsG.toDouble,
            fatG     = profile.targetFatG.toDouble,
            fiberG   = profile.targetFiberG.toDouble,
          )
          val todayMacros = AggregateService.getTodayMacros(userId)
          val estimate    = ClaudeService.estimateMealMacros(description, photo, targets, todayMacros)
          val rawJson     = write(estimate)

          val (mealId, loggedAt) = Database.withConnection { conn =>
            val st = conn.prepareStatement(
              """INSERT INTO meals (user_id, description, has_photo, kcal, protein_g, carbs_g, fat_g, fiber_g, raw_estimate)
                |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                |RETURNING id, logged_at""".stripMargin
            )
            st.setString(1, userId)
            description match
              case Some(d) => st.setString(2, d)
              case None    => st.setNull(2, java.sql.Types.VARCHAR)
            st.setBoolean(3, photo.isDefined)
            st.setInt(4, estimate.kcal)
            st.setDouble(5, estimate.proteinG)
            st.setDouble(6, estimate.carbsG)
            st.setDouble(7, estimate.fatG)
            st.setDouble(8, estimate.fiberG)
            st.setString(9, rawJson)
            val rs = st.executeQuery()
            rs.next()
            (rs.getString("id"), rs.getString("logged_at"))
          }

          ok(
            ujson.Obj(
              "meal" -> ujson.Obj(
                "id"          -> mealId,
                "userId"      -> userId,
                "loggedAt"    -> loggedAt,
                "description" -> description.map(ujson.Str.apply).getOrElse(ujson.Null),
                "hasPhoto"    -> photo.isDefined,
                "kcal"        -> estimate.kcal,
                "proteinG"    -> estimate.proteinG,
                "carbsG"      -> estimate.carbsG,
                "fatG"        -> estimate.fatG,
                "fiberG"      -> estimate.fiberG,
              ),
              "estimate" -> ujson.Obj(
                "kcal"        -> estimate.kcal,
                "proteinG"    -> estimate.proteinG,
                "carbsG"      -> estimate.carbsG,
                "fatG"        -> estimate.fatG,
                "fiberG"      -> estimate.fiberG,
                "description" -> estimate.description,
              ),
            ),
            status = 201,
          )

      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  @cask.get("/api/meals/today")
  def todayMeals(request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val meals = Database.withConnection { conn =>
          val st = conn.prepareStatement(
            """SELECT id, logged_at, description, has_photo, kcal, protein_g, carbs_g, fat_g, fiber_g
              |FROM meals
              |WHERE user_id = ?
              |  AND logged_at >= date_trunc('day', now() AT TIME ZONE 'UTC')
              |ORDER BY logged_at ASC""".stripMargin
          )
          st.setString(1, userId)
          val rs  = st.executeQuery()
          val buf = scala.collection.mutable.ArrayBuffer[ujson.Obj]()
          while rs.next() do
            val kcalVal     = rs.getInt("kcal")
            val kcalOpt     = if rs.wasNull() then ujson.Null else ujson.Num(kcalVal)
            val proteinVal  = rs.getDouble("protein_g")
            val proteinOpt  = if rs.wasNull() then ujson.Null else ujson.Num(proteinVal)
            val carbsVal    = rs.getDouble("carbs_g")
            val carbsOpt    = if rs.wasNull() then ujson.Null else ujson.Num(carbsVal)
            val fatVal      = rs.getDouble("fat_g")
            val fatOpt      = if rs.wasNull() then ujson.Null else ujson.Num(fatVal)
            val fiberVal    = rs.getDouble("fiber_g")
            val fiberOpt    = if rs.wasNull() then ujson.Null else ujson.Num(fiberVal)
            val descStr     = rs.getString("description")
            val descOpt     = if rs.wasNull() then ujson.Null else ujson.Str(descStr)
            buf += ujson.Obj(
              "id"          -> rs.getString("id"),
              "loggedAt"    -> rs.getString("logged_at"),
              "description" -> descOpt,
              "hasPhoto"    -> rs.getBoolean("has_photo"),
              "kcal"        -> kcalOpt,
              "proteinG"    -> proteinOpt,
              "carbsG"      -> carbsOpt,
              "fatG"        -> fatOpt,
              "fiberG"      -> fiberOpt,
            )
          buf.toList
        }

        val totals = AggregateService.getTodayMacros(userId)

        ok(ujson.Obj(
          "meals" -> ujson.Arr(meals*),
          "totals" -> ujson.Obj(
            "kcal"     -> totals.kcal,
            "proteinG" -> totals.proteinG,
            "carbsG"   -> totals.carbsG,
            "fatG"     -> totals.fatG,
            "fiberG"   -> totals.fiberG,
          ),
        ))

      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  initialize()
