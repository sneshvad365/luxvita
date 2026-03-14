package routes

import db.Database
import services.AggregateService

object WeightRoutes extends BaseRoutes:

  @cask.post("/api/weight")
  def logWeight(request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val body     = ujson.read(request.text())
        val weightKg = body("weightKg").num

        val (entryId, loggedAt) = Database.withConnection { conn =>
          val st = conn.prepareStatement(
            """INSERT INTO weight_logs (user_id, weight_kg)
              |VALUES (?::uuid, ?)
              |RETURNING id, logged_at""".stripMargin
          )
          st.setString(1, userId)
          st.setDouble(2, weightKg)
          val rs = st.executeQuery()
          rs.next()
          (rs.getString("id"), rs.getString("logged_at"))
        }

        ok(
          ujson.Obj(
            "id"       -> entryId,
            "weightKg" -> weightKg,
            "loggedAt" -> loggedAt,
          ),
          status = 201,
        )

      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  @cask.get("/api/weight/trend")
  def weightTrend(request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        case class TrendRow(id: String, loggedAt: String, weightKg: Double, rolling7d: Double)

        val rows = Database.withConnection { conn =>
          val st = conn.prepareStatement(
            """WITH ordered AS (
              |  SELECT id,
              |         logged_at::text AS logged_at,
              |         weight_kg::float8 AS weight_kg,
              |         AVG(weight_kg) OVER (
              |           ORDER BY logged_at ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
              |         )::float8 AS rolling_7day
              |  FROM weight_logs
              |  WHERE user_id = ?::uuid
              |)
              |SELECT * FROM ordered ORDER BY logged_at DESC LIMIT 90""".stripMargin
          )
          st.setString(1, userId)
          val rs  = st.executeQuery()
          val buf = scala.collection.mutable.ArrayBuffer[TrendRow]()
          while rs.next() do
            buf += TrendRow(
              id        = rs.getString("id"),
              loggedAt  = rs.getString("logged_at"),
              weightKg  = rs.getDouble("weight_kg"),
              rolling7d = rs.getDouble("rolling_7day"),
            )
          buf.toList
        }

        val sevenDayAvg: ujson.Value =
          if rows.isEmpty then ujson.Null
          else ujson.Num(rows.head.rolling7d)

        // Pace: kg/week from last to first entry (rows are DESC, so last entry = rows.last)
        val paceKgPerWeek: ujson.Value =
          if rows.size < 2 then ujson.Null
          else
            val newest = rows.head
            val oldest = rows.last
            // We need epoch to compute days between entries
            Database.withConnection { conn =>
              val st = conn.prepareStatement(
                """SELECT EXTRACT(EPOCH FROM (
                  |  (SELECT logged_at FROM weight_logs WHERE id = ?::uuid)
                  |  - (SELECT logged_at FROM weight_logs WHERE id = ?::uuid)
                  |)) AS diff_seconds""".stripMargin
              )
              st.setString(1, newest.id)
              st.setString(2, oldest.id)
              val rs = st.executeQuery()
              rs.next()
              val diffSeconds = rs.getDouble("diff_seconds")
              if diffSeconds <= 0 then ujson.Null
              else
                val diffWeeks = diffSeconds / (7.0 * 24 * 3600)
                val pace = (newest.weightKg - oldest.weightKg) / diffWeeks
                ujson.Num(BigDecimal(pace).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble)
            }

        // ETA to goal weight
        val etaDays: ujson.Value =
          paceKgPerWeek match
            case ujson.Null => ujson.Null
            case paceVal =>
              val profile = AggregateService.getProfile(userId)
              profile.flatMap(_.goalWeightKg) match
                case None => ujson.Null
                case Some(goalKg) =>
                  if rows.isEmpty then ujson.Null
                  else
                    val currentKg = rows.head.weightKg
                    val pace      = paceVal.num
                    if pace == 0 then ujson.Null
                    else
                      val weeksToGoal = (goalKg - currentKg) / pace
                      val days        = (weeksToGoal * 7).toInt
                      if days < 0 then ujson.Null else ujson.Num(days)

        val entries = rows.map { r =>
          ujson.Obj(
            "loggedAt"  -> r.loggedAt,
            "weightKg"  -> r.weightKg,
            "rolling7d" -> r.rolling7d,
          )
        }

        ok(ujson.Obj(
          "entries"       -> ujson.Arr(entries*),
          "sevenDayAvg"   -> sevenDayAvg,
          "paceKgPerWeek" -> paceKgPerWeek,
          "etaDays"       -> etaDays,
        ))

      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  initialize()
