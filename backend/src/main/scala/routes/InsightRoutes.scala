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

        if !isToday then
          // Past days: return cached or empty
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
              ok(ujson.Obj("insight" -> ujson.Null, "type" -> ujson.Null))
        else
          val profile        = AggregateService.getProfileOrDefault(userId)
          val targets        = models.Macros(
            kcal          = profile.targetKcal,
            proteinG      = profile.targetProteinG.toDouble,
            carbsG        = profile.targetCarbsG.toDouble,
            fatG          = profile.targetFatG.toDouble,
            saturatedFatG = profile.targetSaturatedFatG.toDouble,
            fiberG        = profile.targetFiberG.toDouble,
          )
          val todayMacros    = AggregateService.getMacrosForDate(userId, resolvedDate)
          val activityStr    = AggregateService.getActivityStringForDate(userId, resolvedDate)
          val weightKg       = AggregateService.getLatestWeightKg(userId)
          val sevenDayAvg    = AggregateService.getNDayAvgMacros(userId, 7)
          val patterns       = AggregateService.patternNotes(userId, profile)
          val medicalContext = AggregateService.getMedicalContext(userId)

          val insight = ClaudeService.dailyInsight(
            profile        = profile,
            targets        = targets,
            today          = todayMacros,
            activityStr    = activityStr,
            weightKg       = weightKg,
            sevenDayAvg    = sevenDayAvg,
            patterns       = patterns,
            medicalContext = medicalContext,
          )

          // Persist (upsert — always overwrite with latest)
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

          // Auto-trigger weekly insights on Sundays
          val dayOfWeek = java.time.LocalDate.parse(resolvedDate).getDayOfWeek
          if dayOfWeek == java.time.DayOfWeek.SUNDAY then
            generateAndSaveWeeklyInsights(userId, resolvedDate)

          ok(ujson.Obj("insight" -> insight.insight, "type" -> insight.`type`, "cached" -> false))

      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  @cask.get("/api/insights/weekly")
  def weeklyInsights(date: Option[String] = None, request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        // Find most recent Sunday on or before the requested date
        val refDate  = date.filter(_.nonEmpty).map(java.time.LocalDate.parse)
          .getOrElse(java.time.LocalDate.now(java.time.ZoneOffset.UTC))
        val daysBack = refDate.getDayOfWeek.getValue % 7  // Sunday = 0
        val sunday   = refDate.minusDays(daysBack)

        // Return cached weekly insights for that Sunday if available
        val cached = Database.withConnection { conn =>
          val st = conn.prepareStatement(
            """SELECT insight, type, rank FROM weekly_insights
              |WHERE user_id = ?::uuid AND week_date = ?::date
              |ORDER BY rank ASC""".stripMargin
          )
          st.setString(1, userId)
          st.setString(2, sunday.toString)
          val rs  = st.executeQuery()
          val buf = scala.collection.mutable.ArrayBuffer[ujson.Obj]()
          while rs.next() do
            buf += ujson.Obj("insight" -> rs.getString("insight"), "type" -> rs.getString("type"))
          buf.toList
        }

        if cached.nonEmpty then
          ok(ujson.Arr(cached*))
        else
          ok(ujson.Arr())

      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  @cask.post("/api/insights/chat")
  def insightChat(request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val body     = ujson.read(request.text())
        val messages = body("messages").arr.map(m => (m("role").str, m("content").str)).toList
        val dateStr  = body.obj.get("date").flatMap(v => if v.isNull then None else Some(v.str))
          .filter(_.nonEmpty).getOrElse(todayStr)
        val profile        = AggregateService.getProfileOrDefault(userId)
        val targets        = models.Macros(
          kcal          = profile.targetKcal,
          proteinG      = profile.targetProteinG.toDouble,
          carbsG        = profile.targetCarbsG.toDouble,
          fatG          = profile.targetFatG.toDouble,
          saturatedFatG = profile.targetSaturatedFatG.toDouble,
          fiberG        = profile.targetFiberG.toDouble,
        )
        val todayMacros    = AggregateService.getMacrosForDate(userId, dateStr)
        val activityStr    = AggregateService.getActivityStringForDate(userId, dateStr)
        val weightKg       = AggregateService.getLatestWeightKg(userId)
        val medicalContext = AggregateService.getMedicalContext(userId)
        val reply = ClaudeService.dailyInsightChat(
          profile        = profile,
          targets        = targets,
          today          = todayMacros,
          activityStr    = activityStr,
          weightKg       = weightKg,
          medicalContext = medicalContext,
          messages       = messages,
        )
        ok(ujson.Obj("reply" -> reply))
      catch
        case e: Exception => err("SERVER_ERROR", e.getMessage, 500)
    }

  private def generateAndSaveWeeklyInsights(userId: String, weekDate: String): Unit =
    // Skip if already generated for this Sunday
    val alreadyExists = Database.withConnection { conn =>
      val st = conn.prepareStatement(
        "SELECT 1 FROM weekly_insights WHERE user_id = ?::uuid AND week_date = ?::date LIMIT 1"
      )
      st.setString(1, userId)
      st.setString(2, weekDate)
      st.executeQuery().next()
    }
    if !alreadyExists then
      val profile      = AggregateService.getProfileOrDefault(userId)
      val targets      = models.Macros(
        kcal          = profile.targetKcal,
        proteinG      = profile.targetProteinG.toDouble,
        carbsG        = profile.targetCarbsG.toDouble,
        fatG          = profile.targetFatG.toDouble,
        saturatedFatG = profile.targetSaturatedFatG.toDouble,
        fiberG        = profile.targetFiberG.toDouble,
      )
      val thirtyDayAvg   = AggregateService.getNDayAvgMacros(userId, 30)
      val (kcalC, protC, fibC) = AggregateService.consistencyScores(userId, profile)
      val consistencyStr = s"kcal: ${f"${kcalC * 100}%.0f"}%, protein: ${f"${protC * 100}%.0f"}%, fiber: ${f"${fibC * 100}%.0f"}%"
      val patterns       = AggregateService.patternNotes(userId, profile)
      val latestWeight   = AggregateService.getLatestWeightKg(userId)
      val weightTrendStr = latestWeight match
        case None    => "No weight data available."
        case Some(w) => profile.goalWeightKg match
          case None       => s"Current weight: ${w}kg. No goal weight set."
          case Some(goal) => s"Current weight: ${w}kg. Goal: ${goal}kg."
      val medicalContext = AggregateService.getMedicalContext(userId)

      val insights = ClaudeService.weeklyInsights(
        profile        = profile,
        targets        = targets,
        thirtyDayAvg   = thirtyDayAvg,
        weightTrend    = weightTrendStr,
        consistency    = consistencyStr,
        patterns       = patterns,
        medicalContext = medicalContext,
      )

      Database.withConnection { conn =>
        insights.take(3).zipWithIndex.foreach { case (insight, idx) =>
          val st = conn.prepareStatement(
            """INSERT INTO weekly_insights (user_id, week_date, insight, type, rank)
              |VALUES (?::uuid, ?::date, ?, ?, ?)
              |ON CONFLICT (user_id, week_date, rank) DO NOTHING""".stripMargin
          )
          st.setString(1, userId)
          st.setString(2, weekDate)
          st.setString(3, insight.insight)
          st.setString(4, insight.`type`)
          st.setInt(5, idx + 1)
          st.executeUpdate()
        }
      }

  private def todayStr: String =
    java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString

  initialize()
