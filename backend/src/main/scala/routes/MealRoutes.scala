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

          val breakdownJson = write(estimate.breakdown)

          val (mealId, loggedAt) = Database.withConnection { conn =>
            val st = conn.prepareStatement(
              """INSERT INTO meals (user_id, description, has_photo, kcal, protein_g, carbs_g, fat_g, fiber_g, raw_estimate, breakdown, photo_data)
                |VALUES (?::uuid, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?)
                |RETURNING id, logged_at""".stripMargin
            )
            st.setString(1, userId)
            val storedDescription = description.orElse(Some(estimate.description).filter(_.nonEmpty))
            storedDescription match
              case Some(d) => st.setString(2, d)
              case None    => st.setNull(2, java.sql.Types.VARCHAR)
            st.setBoolean(3, photo.isDefined)
            st.setInt(4, estimate.kcal)
            st.setDouble(5, estimate.proteinG)
            st.setDouble(6, estimate.carbsG)
            st.setDouble(7, estimate.fatG)
            st.setDouble(8, estimate.fiberG)
            st.setString(9, rawJson)
            st.setString(10, breakdownJson)
            photo match
              case Some(p) => st.setString(11, p)
              case None    => st.setNull(11, java.sql.Types.VARCHAR)
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
                "waterMl"     -> estimate.waterMl.map(ujson.Num(_)).getOrElse(ujson.Null),
              ),
            ),
            status = 201,
          )

      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  @cask.post("/api/meals/copy")
  def copyMeal(request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val body   = ujson.read(request.text())
        val mealId = body("mealId").str

        val source = Database.withConnection { conn =>
          val st = conn.prepareStatement(
            """SELECT description, kcal, protein_g, carbs_g, fat_g, fiber_g, breakdown
              |FROM meals WHERE id = ?::uuid AND user_id = ?::uuid""".stripMargin
          )
          st.setString(1, mealId)
          st.setString(2, userId)
          val rs = st.executeQuery()
          if rs.next() then
            val desc      = Option(rs.getString("description"))
            val kcal      = rs.getInt("kcal")
            val proteinG  = rs.getDouble("protein_g")
            val carbsG    = rs.getDouble("carbs_g")
            val fatG      = rs.getDouble("fat_g")
            val fiberG    = rs.getDouble("fiber_g")
            val breakdown = Option(rs.getString("breakdown"))
            Some((desc, kcal, proteinG, carbsG, fatG, fiberG, breakdown))
          else None
        }

        source match
          case None => err("NOT_FOUND", "Meal not found", 404)
          case Some((desc, kcal, proteinG, carbsG, fatG, fiberG, breakdown)) =>
            Database.withConnection { conn =>
              val st = conn.prepareStatement(
                """INSERT INTO meals (user_id, description, kcal, protein_g, carbs_g, fat_g, fiber_g, breakdown)
                  |VALUES (?::uuid, ?, ?, ?, ?, ?, ?, ?::jsonb)
                  |RETURNING id""".stripMargin
              )
              st.setString(1, userId)
              desc match
                case Some(d) => st.setString(2, d)
                case None    => st.setNull(2, java.sql.Types.VARCHAR)
              st.setInt(3, kcal)
              st.setDouble(4, proteinG)
              st.setDouble(5, carbsG)
              st.setDouble(6, fatG)
              st.setDouble(7, fiberG)
              breakdown match
                case Some(b) => st.setString(8, b)
                case None    => st.setNull(8, java.sql.Types.VARCHAR)
              st.executeQuery().next()
            }
            val breakdownJson = breakdown.map(b => ujson.read(b)).getOrElse(ujson.Arr())
            ok(ujson.Obj(
              "estimate" -> ujson.Obj(
                "kcal"        -> kcal,
                "proteinG"    -> proteinG,
                "carbsG"      -> carbsG,
                "fatG"        -> fatG,
                "fiberG"      -> fiberG,
                "description" -> desc.getOrElse("Copied meal"),
                "waterMl"     -> ujson.Null,
                "breakdown"   -> breakdownJson,
              )
            ), status = 201)
      catch
        case e: Exception => err("SERVER_ERROR", e.getMessage, 500)
    }

  @cask.put("/api/meals/:id")
  def updateMeal(id: String, request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val body        = ujson.read(request.text())
        val description = body.obj.get("description").flatMap(v => if v.isNull then None else Some(v.str)).filter(_.nonEmpty)
        val kcal        = body("kcal").num.toInt
        val proteinG    = body("proteinG").num
        val carbsG      = body("carbsG").num
        val fatG        = body("fatG").num
        val fiberG      = body("fiberG").num

        val updated = Database.withConnection { conn =>
          val st = conn.prepareStatement(
            """UPDATE meals SET description = ?, kcal = ?, protein_g = ?, carbs_g = ?, fat_g = ?, fiber_g = ?
              |WHERE id = ?::uuid AND user_id = ?::uuid
              |RETURNING id""".stripMargin
          )
          description match
            case Some(d) => st.setString(1, d)
            case None    => st.setNull(1, java.sql.Types.VARCHAR)
          st.setInt(2, kcal)
          st.setDouble(3, proteinG)
          st.setDouble(4, carbsG)
          st.setDouble(5, fatG)
          st.setDouble(6, fiberG)
          st.setString(7, id)
          st.setString(8, userId)
          st.executeQuery().next()
        }
        if updated then
          ok(ujson.Obj(
            "id"          -> id,
            "description" -> description.map(ujson.Str.apply).getOrElse(ujson.Null),
            "kcal"        -> kcal,
            "proteinG"    -> proteinG,
            "carbsG"      -> carbsG,
            "fatG"        -> fatG,
            "fiberG"      -> fiberG,
          ))
        else
          err("NOT_FOUND", "Meal not found", 404)
      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  @cask.delete("/api/meals/:id")
  def deleteMeal(id: String, request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val deleted = Database.withConnection { conn =>
          val st = conn.prepareStatement(
            "DELETE FROM meals WHERE id = ?::uuid AND user_id = ?::uuid RETURNING id"
          )
          st.setString(1, id)
          st.setString(2, userId)
          st.executeQuery().next()
        }
        if deleted then ok(ujson.Obj("id" -> id))
        else err("NOT_FOUND", "Meal not found", 404)
      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  @cask.get("/api/meals/today")
  def todayMeals(date: Option[String] = None, request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val resolvedDate = date.filter(_.nonEmpty)
          .getOrElse(java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString)
        val meals = Database.withConnection { conn =>
          val st = conn.prepareStatement(
            """SELECT id, logged_at, description, has_photo, kcal, protein_g, carbs_g, fat_g, fiber_g, breakdown
              |FROM meals
              |WHERE user_id = ?::uuid
              |  AND logged_at >= ?::date
              |  AND logged_at <  ?::date + INTERVAL '1 day'
              |ORDER BY logged_at ASC""".stripMargin
          )
          st.setString(1, userId)
          st.setString(2, resolvedDate)
          st.setString(3, resolvedDate)
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
            val breakdownStr = rs.getString("breakdown")
            val breakdownVal = if rs.wasNull() then ujson.Arr() else ujson.read(breakdownStr)
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
              "breakdown"   -> breakdownVal,
            )
          buf.toList
        }

        val totals = AggregateService.getMacrosForDate(userId, resolvedDate)

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
