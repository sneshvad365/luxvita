package routes

import services.{AggregateService, ClaudeService}

object InsightRoutes extends BaseRoutes:

  @cask.get("/api/insights/daily")
  def dailyInsight(request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val profile     = AggregateService.getProfileOrDefault(userId)
        val targets     = models.Macros(
          kcal     = profile.targetKcal,
          proteinG = profile.targetProteinG.toDouble,
          carbsG   = profile.targetCarbsG.toDouble,
          fatG     = profile.targetFatG.toDouble,
          fiberG   = profile.targetFiberG.toDouble,
        )
        val todayMacros = AggregateService.getTodayMacros(userId)
        val activityStr = AggregateService.getTodayActivityString(userId)
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

        ok(ujson.Obj(
          "insight" -> insight.insight,
          "type"    -> insight.`type`,
        ))

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

  initialize()
