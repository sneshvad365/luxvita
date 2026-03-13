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
                |VALUES (?, ?, ?::jsonb)
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

  initialize()
