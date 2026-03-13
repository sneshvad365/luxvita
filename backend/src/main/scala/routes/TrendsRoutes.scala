package routes

import services.AggregateService

object TrendsRoutes extends BaseRoutes:

  @cask.get("/api/trends/summary")
  def trendsSummary(request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val profile      = AggregateService.getProfileOrDefault(userId)
        val sevenDayAvg  = AggregateService.getNDayAvgMacros(userId, 7)
        val thirtyDayAvg = AggregateService.getNDayAvgMacros(userId, 30)
        val (kcalC, protC, fibC) = AggregateService.consistencyScores(userId, profile)

        def macrosToJson(m: models.Macros): ujson.Obj =
          ujson.Obj(
            "kcal"     -> m.kcal,
            "proteinG" -> m.proteinG,
            "carbsG"   -> m.carbsG,
            "fatG"     -> m.fatG,
            "fiberG"   -> m.fiberG,
          )

        ok(ujson.Obj(
          "sevenDayAvg"  -> macrosToJson(sevenDayAvg),
          "thirtyDayAvg" -> macrosToJson(thirtyDayAvg),
          "consistency"  -> ujson.Obj(
            "kcal"    -> BigDecimal(kcalC).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble,
            "protein" -> BigDecimal(protC).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble,
            "fiber"   -> BigDecimal(fibC).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble,
          ),
        ))

      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  initialize()
