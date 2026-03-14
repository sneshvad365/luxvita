package routes

import db.Database
import services.{AggregateService, ClaudeService}

object InsightRoutes extends BaseRoutes:

  // GET /api/insights/daily?date=2026-03-14  (date optional, defaults to today)
  @cask.get("/api/insights/daily")
  def dailyInsight(date: Option[String] = None, request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val resolvedDate = date.filter(_.nonEmpty).getOrElse(todayStr)
        val isToday      = resolvedDate == todayStr

        // Return cached insight if it exists
        val cached = Database.withConnection { conn =>
          val st = conn.prepareStatement(
            "SELECT insight, type FROM daily_insights WHERE user_id = ?::uuid AND date = ?::date"
          )
          st.setString(1, userId)
          st.setString(2, resolvedDate)
          val rs = st.executeQuery()
          if rs.next() then Some((rs.getString("insight"), rs.getString("type"))) else None
        }

        cached match
          case Some((insightText, insightType)) =>
            ok(ujson.Obj("insight" -> insightText, "type" -> insightType, "cached" -> true))

          case None =>
            // Only generate via Claude for today — past days without saved insight return empty
            if !isToday then
              ok(ujson.Obj("insight" -> ujson.Null, "type" -> ujson.Null))
            else
              val profile     = AggregateService.getProfileOrDefault(userId)
              val targets     = models.Macros(
                kcal     = profile.targetKcal,
                proteinG = profile.targetProteinG.toDouble,
                carbsG   = profile.targetCarbsG.toDouble,
                fatG     = profile.targetFatG.toDouble,
                fiberG   = profile.targetFiberG.toDouble,
              )
              val todayMacros = AggregateService.getMacrosForDate(userId, resolvedDate)
              val activityStr = AggregateService.getActivityStringForDate(userId, resolvedDate)
              val weightKg    = AggregateService.getLatestWeightKg(userId)
              val sevenDayAvg = AggregateService.getNDayAvgMacros(userId, 7)
              val patterns    = AggregateService.patternNotes(userId, profile)

              val insight = ClaudeService.dailyInsight(
                profile     = profile,
                targets     = targets,
                today       = todayMacros,
                activityStr = activityStr,
                weightKg    = weightKg,
                sevenDayAvg = sevenDayAvg,
                patterns    = patterns,
              )

              // Persist
              Database.withConnection { conn =>
                val st = conn.prepareStatement(
                  """INSERT INTO daily_insights (user_id, date, insight, type)
                    |VALUES (?::uuid, ?::date, ?, ?)
                    |ON CONFLICT (user_id, date) DO UPDATE SET insight = EXCLUDED.insight, type = EXCLUDED.type""".stripMargin
                )
                st.setString(1, userId)
                st.setString(2, resolvedDate)
                st.setString(3, insight.insight)
                st.setString(4, insight.`type`)
                st.executeUpdate()
              }

              ok(ujson.Obj("insight" -> insight.insight, "type" -> insight.`type`, "cached" -> false))

      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  @cask.get("/api/insights/weekly")
  def weeklyInsights(request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val profile      = AggregateService.getProfileOrDefault(userId)
        val targets      = models.Macros(
          kcal     = profile.targetKcal,
          proteinG = profile.targetProteinG.toDouble,
          carbsG   = profile.targetCarbsG.toDouble,
          fatG     = profile.targetFatG.toDouble,
          fiberG   = profile.targetFiberG.toDouble,
        )
        val thirtyDayAvg = AggregateService.getNDayAvgMacros(userId, 30)
        val (kcalC, protC, fibC) = AggregateService.consistencyScores(userId, profile)
        val consistencyStr = s"kcal: ${f"${kcalC * 100}%.0f"}%, protein: ${f"${protC * 100}%.0f"}%, fiber: ${f"${fibC * 100}%.0f"}%"
        val patterns       = AggregateService.patternNotes(userId, profile)
        val latestWeight   = AggregateService.getLatestWeightKg(userId)
        val weightTrendStr = latestWeight match
          case None    => "No weight data available."
          case Some(w) => profile.goalWeightKg match
            case None       => s"Current weight: ${w}kg. No goal weight set."
            case Some(goal) => s"Current weight: ${w}kg. Goal: ${goal}kg."

        val insights = ClaudeService.weeklyInsights(
          profile      = profile,
          targets      = targets,
          thirtyDayAvg = thirtyDayAvg,
          weightTrend  = weightTrendStr,
          consistency  = consistencyStr,
          patterns     = patterns,
        )

        val arr = insights.take(3).map { i =>
          ujson.Obj("insight" -> i.insight, "type" -> i.`type`)
        }
        ok(ujson.Arr(arr*))

      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  private def todayStr: String =
    java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString

  initialize()
