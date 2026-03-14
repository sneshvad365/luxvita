package routes

import db.Database

object WaterRoutes extends BaseRoutes:

  @cask.post("/api/water")
  def logWater(request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val body     = ujson.read(request.text())
        val amountL  = body("amountL").num

        val (entryId, loggedAt) = Database.withConnection { conn =>
          val st = conn.prepareStatement(
            """INSERT INTO water_logs (user_id, amount_l)
              |VALUES (?::uuid, ?)
              |RETURNING id, logged_at""".stripMargin
          )
          st.setString(1, userId)
          st.setDouble(2, amountL)
          val rs = st.executeQuery()
          rs.next()
          (rs.getString("id"), rs.getString("logged_at"))
        }

        ok(ujson.Obj("id" -> entryId, "amountL" -> amountL, "loggedAt" -> loggedAt), status = 201)
      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  @cask.get("/api/water/today")
  def todayWater(date: Option[String] = None, request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val resolvedDate = date.filter(_.nonEmpty)
          .getOrElse(java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString)
        val (totalL, entries) = Database.withConnection { conn =>
          val st = conn.prepareStatement(
            """SELECT id, logged_at, amount_l::float8
              |FROM water_logs
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
          var sum = 0.0
          while rs.next() do
            val a = rs.getDouble("amount_l")
            sum += a
            buf += ujson.Obj(
              "id"       -> rs.getString("id"),
              "loggedAt" -> rs.getString("logged_at"),
              "amountL"  -> a,
            )
          (sum, buf.toList)
        }

        ok(ujson.Obj(
          "totalL"  -> totalL,
          "entries" -> ujson.Arr(entries*),
        ))
      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  @cask.delete("/api/water/:id")
  def deleteWater(id: String, request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val deleted = Database.withConnection { conn =>
          val st = conn.prepareStatement(
            "DELETE FROM water_logs WHERE id = ?::uuid AND user_id = ?::uuid RETURNING id"
          )
          st.setString(1, id)
          st.setString(2, userId)
          st.executeQuery().next()
        }
        if deleted then ok(ujson.Obj("id" -> id))
        else err("NOT_FOUND", "Entry not found", 404)
      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  initialize()
