package routes

import db.Database
import services.ClaudeService
import upickle.default.*

object ActivityRoutes extends BaseRoutes:

  @cask.post("/api/activity")
  def logActivity(request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val body  = ujson.read(request.text())
        val entry = body("entry").str.trim

        if entry.isEmpty then
          err("INVALID_INPUT", "Activity entry is required", 400)
        else
          val parsed    = ClaudeService.parseActivity(entry)
          val parsedJson = write(parsed)

          val (actId, loggedAt) = Database.withConnection { conn =>
            val st = conn.prepareStatement(
              """INSERT INTO activity_logs (user_id, entry, parsed)
                |VALUES (?::uuid, ?, ?::jsonb)
                |RETURNING id, logged_at""".stripMargin
            )
            st.setString(1, userId)
            st.setString(2, entry)
            st.setString(3, parsedJson)
            val rs = st.executeQuery()
            rs.next()
            (rs.getString("id"), rs.getString("logged_at"))
          }

          ok(
            ujson.Obj(
              "id"       -> actId,
              "entry"    -> entry,
              "parsed"   -> ujson.Obj(
                "type"        -> parsed.`type`,
                "durationMin" -> parsed.durationMin.map(ujson.Num(_)).getOrElse(ujson.Null),
                "intensity"   -> parsed.intensity.map(ujson.Str.apply).getOrElse(ujson.Null),
                "steps"       -> parsed.steps.map(ujson.Num(_)).getOrElse(ujson.Null),
                "mood"        -> parsed.mood.map(ujson.Str.apply).getOrElse(ujson.Null),
                "notes"       -> parsed.notes.map(ujson.Str.apply).getOrElse(ujson.Null),
              ),
              "loggedAt" -> loggedAt,
            ),
            status = 201,
          )

      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  @cask.put("/api/activity/:id")
  def updateActivity(id: String, request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val body  = ujson.read(request.text())
        val entry = body("entry").str.trim
        if entry.isEmpty then
          err("INVALID_INPUT", "Activity entry is required", 400)
        else
          val parsed     = ClaudeService.parseActivity(entry)
          val parsedJson = write(parsed)
          val updated = Database.withConnection { conn =>
            val st = conn.prepareStatement(
              """UPDATE activity_logs SET entry = ?, parsed = ?::jsonb
                |WHERE id = ?::uuid AND user_id = ?::uuid
                |RETURNING id""".stripMargin
            )
            st.setString(1, entry)
            st.setString(2, parsedJson)
            st.setString(3, id)
            st.setString(4, userId)
            val rs = st.executeQuery()
            rs.next()
          }
          if updated then
            ok(ujson.Obj(
              "id"     -> id,
              "entry"  -> entry,
              "parsed" -> ujson.Obj(
                "type"        -> parsed.`type`,
                "durationMin" -> parsed.durationMin.map(ujson.Num(_)).getOrElse(ujson.Null),
                "intensity"   -> parsed.intensity.map(ujson.Str.apply).getOrElse(ujson.Null),
                "steps"       -> parsed.steps.map(ujson.Num(_)).getOrElse(ujson.Null),
                "mood"        -> parsed.mood.map(ujson.Str.apply).getOrElse(ujson.Null),
                "notes"       -> parsed.notes.map(ujson.Str.apply).getOrElse(ujson.Null),
              ),
            ))
          else
            err("NOT_FOUND", "Activity not found", 404)
      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  @cask.delete("/api/activity/:id")
  def deleteActivity(id: String, request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val deleted = Database.withConnection { conn =>
          val st = conn.prepareStatement(
            "DELETE FROM activity_logs WHERE id = ?::uuid AND user_id = ?::uuid RETURNING id"
          )
          st.setString(1, id)
          st.setString(2, userId)
          st.executeQuery().next()
        }
        if deleted then ok(ujson.Obj("id" -> id))
        else err("NOT_FOUND", "Activity not found", 404)
      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  @cask.get("/api/activity/today")
  def todayActivity(date: Option[String] = None, request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val resolvedDate = date.filter(_.nonEmpty)
          .getOrElse(java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString)
        val activities = Database.withConnection { conn =>
          val st = conn.prepareStatement(
            """SELECT id, logged_at, entry, parsed
              |FROM activity_logs
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
            val parsedStr = rs.getString("parsed")
            val parsedVal = if rs.wasNull() then ujson.Null else ujson.read(parsedStr)
            buf += ujson.Obj(
              "id"       -> rs.getString("id"),
              "loggedAt" -> rs.getString("logged_at"),
              "entry"    -> rs.getString("entry"),
              "parsed"   -> parsedVal,
            )
          buf.toList
        }
        ok(ujson.Obj("activities" -> ujson.Arr(activities*)))
      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  initialize()
