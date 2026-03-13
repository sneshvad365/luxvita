package services

import models.{MacroEstimate, ParsedActivity, Insight, Macros, UserProfile}
import java.net.{URI, http as jhttp}
import java.net.http.HttpRequest.BodyPublishers
import java.time.Duration

/** All calls to the Anthropic Claude API live here. No Claude calls elsewhere. */
object ClaudeService:

  private val conf      = com.typesafe.config.ConfigFactory.load()
  private val apiKey    = sys.env.getOrElse("ANTHROPIC_API_KEY", conf.getString("anthropic.api-key"))
  private val model     = "claude-sonnet-4-20250514"
  private val apiUrl    = "https://api.anthropic.com/v1/messages"
  private val httpClient = jhttp.HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build()

  // ---------------------------------------------------------------------------
  // Public API
  // ---------------------------------------------------------------------------

  def estimateMealMacros(
    description : Option[String],
    photoBase64 : Option[String],
    targets     : Macros,
    todayTotals : Macros,
  ): MacroEstimate =
    val prompt = buildMealPrompt(description, targets, todayTotals)
    val content = buildMessageContent(description, photoBase64, prompt)
    val responseText = callClaude(systemMsg = mealSystem, content = content)
    parseMacroEstimate(responseText)

  def parseActivity(entry: String): ParsedActivity =
    val prompt = s"""Parse this activity log entry into structured JSON.
      |Entry: "$entry"
      |Return ONLY valid JSON: { "type": string, "duration_min": int|null, "intensity": "low"|"moderate"|"high"|null, "steps": int|null, "mood": string|null, "notes": string|null }
      |""".stripMargin
    val responseText = callClaude(systemMsg = "You extract structured data from text. Return ONLY valid JSON.", content = ujsonTextContent(prompt))
    parseActivityJson(responseText)

  def dailyInsight(
    profile    : UserProfile,
    targets    : Macros,
    today      : Macros,
    activityStr: String,
    weightKg   : Option[Double],
    sevenDayAvg: Macros,
    patterns   : String,
  ): Insight =
    val prompt = buildDailyInsightPrompt(profile, targets, today, activityStr, weightKg, sevenDayAvg, patterns)
    val responseText = callClaude(systemMsg = insightSystem, content = ujsonTextContent(prompt))
    parseInsight(responseText)

  def weeklyInsights(
    profile      : UserProfile,
    targets      : Macros,
    thirtyDayAvg : Macros,
    weightTrend  : String,
    consistency  : String,
    patterns     : String,
  ): List[Insight] =
    val prompt = buildWeeklyInsightPrompt(profile, targets, thirtyDayAvg, weightTrend, consistency, patterns)
    val responseText = callClaude(systemMsg = insightSystem, content = ujsonTextContent(prompt))
    parseInsightArray(responseText)

  // ---------------------------------------------------------------------------
  // Prompt builders
  // ---------------------------------------------------------------------------

  private val mealSystem =
    "You are a nutrition expert. Return ONLY valid JSON, no prose."

  private val insightSystem =
    "You are a personal nutrition coach. Be specific and actionable. Max 2 sentences per insight."

  private def buildMealPrompt(
    description : Option[String],
    targets     : Macros,
    today       : Macros,
  ): String =
    s"""Targets: ${targets.kcal} kcal, ${targets.proteinG}g protein, ${targets.carbsG}g carbs, ${targets.fatG}g fat, ${targets.fiberG}g fiber
       |Already logged today: ${today.kcal} kcal, ${today.proteinG}g protein, ${today.carbsG}g carbs, ${today.fatG}g fat, ${today.fiberG}g fiber
       |${description.map(d => s"New meal: \"$d\"").getOrElse("Meal provided as photo only — estimate from the image.")}
       |
       |Return: { "kcal": int, "protein_g": float, "carbs_g": float, "fat_g": float, "fiber_g": float, "description": string }
       |""".stripMargin

  private def buildMessageContent(
    description : Option[String],
    photoBase64 : Option[String],
    prompt      : String,
  ): ujson.Value =
    photoBase64 match
      case None =>
        ujsonTextContent(prompt)
      case Some(b64) =>
        ujson.Arr(
          ujson.Obj(
            "type"   -> "image",
            "source" -> ujson.Obj(
              "type"       -> "base64",
              "media_type" -> "image/jpeg",
              "data"       -> b64,
            ),
          ),
          ujson.Obj("type" -> "text", "text" -> prompt),
        )

  private def buildDailyInsightPrompt(
    profile    : UserProfile,
    targets    : Macros,
    today      : Macros,
    activityStr: String,
    weightKg   : Option[Double],
    sevenDayAvg: Macros,
    patterns   : String,
  ): String =
    s"""${profile.bio.map(b => s"Profile: \"$b\"").getOrElse("")}
       |Targets: { kcal: ${targets.kcal}, protein: ${targets.proteinG}g, carbs: ${targets.carbsG}g, fat: ${targets.fatG}g, fiber: ${targets.fiberG}g }
       |Today: { kcal: ${today.kcal}, protein: ${today.proteinG}g, carbs: ${today.carbsG}g, fat: ${today.fatG}g, fiber: ${today.fiberG}g }
       |Activity: "$activityStr"
       |${weightKg.map(w => s"Weight today: ${w}kg").getOrElse("No weight logged today.")}
       |7-day averages: { kcal: ${sevenDayAvg.kcal}, protein: ${sevenDayAvg.proteinG}g, fiber: ${sevenDayAvg.fiberG}g }
       |Pattern notes: $patterns
       |
       |Return: { "insight": string, "type": "protein"|"timing"|"fiber"|"hydration"|"recovery"|"weight" }
       |""".stripMargin

  private def buildWeeklyInsightPrompt(
    profile      : UserProfile,
    targets      : Macros,
    thirtyDayAvg : Macros,
    weightTrend  : String,
    consistency  : String,
    patterns     : String,
  ): String =
    s"""${profile.bio.map(b => s"Profile: \"$b\"").getOrElse("")}
       |Targets: { kcal: ${targets.kcal}, protein: ${targets.proteinG}g, carbs: ${targets.carbsG}g, fat: ${targets.fatG}g, fiber: ${targets.fiberG}g }
       |30-day averages: { kcal: ${thirtyDayAvg.kcal}, protein: ${thirtyDayAvg.proteinG}g, carbs: ${thirtyDayAvg.carbsG}g, fat: ${thirtyDayAvg.fatG}g, fiber: ${thirtyDayAvg.fiberG}g }
       |Weight trend: $weightTrend
       |Consistency: $consistency
       |Notable patterns: $patterns
       |
       |Return an array of exactly 3 insights ordered by impact:
       |[{ "insight": string, "type": "protein"|"timing"|"fiber"|"hydration"|"recovery"|"weight" }, ...]
       |""".stripMargin

  // ---------------------------------------------------------------------------
  // HTTP + parsing helpers
  // ---------------------------------------------------------------------------

  private def ujsonTextContent(text: String): ujson.Value =
    ujson.Arr(ujson.Obj("type" -> "text", "text" -> text))

  private def callClaude(systemMsg: String, content: ujson.Value): String =
    val body = ujson.Obj(
      "model"      -> model,
      "max_tokens" -> 1024,
      "system"     -> systemMsg,
      "messages"   -> ujson.Arr(
        ujson.Obj("role" -> "user", "content" -> content)
      ),
    )

    val request = jhttp.HttpRequest.newBuilder()
      .uri(URI.create(apiUrl))
      .header("Content-Type",      "application/json")
      .header("x-api-key",         apiKey)
      .header("anthropic-version", "2023-06-01")
      .POST(BodyPublishers.ofString(ujson.write(body)))
      .timeout(Duration.ofSeconds(30))
      .build()

    val response = httpClient.send(request, jhttp.HttpResponse.BodyHandlers.ofString())
    if response.statusCode() != 200 then
      throw RuntimeException(s"Claude API error ${response.statusCode()}")

    val json = ujson.read(response.body())
    json("content")(0)("text").str

  private def parseMacroEstimate(text: String): MacroEstimate =
    val json = ujson.read(text.trim)
    MacroEstimate(
      kcal       = json("kcal").num.toInt,
      proteinG   = json("protein_g").num,
      carbsG     = json("carbs_g").num,
      fatG       = json("fat_g").num,
      fiberG     = json("fiber_g").num,
      description = json("description").str,
    )

  private def parseActivityJson(text: String): ParsedActivity =
    val json = ujson.read(text.trim)
    ParsedActivity(
      `type`      = json("type").str,
      durationMin = json.obj.get("duration_min").flatMap(v => if v.isNull then None else Some(v.num.toInt)),
      intensity   = json.obj.get("intensity").flatMap(v => if v.isNull then None else Some(v.str)),
      steps       = json.obj.get("steps").flatMap(v => if v.isNull then None else Some(v.num.toInt)),
      mood        = json.obj.get("mood").flatMap(v => if v.isNull then None else Some(v.str)),
      notes       = json.obj.get("notes").flatMap(v => if v.isNull then None else Some(v.str)),
    )

  private def parseInsight(text: String): Insight =
    val json = ujson.read(text.trim)
    Insight(insight = json("insight").str, `type` = json("type").str)

  private def parseInsightArray(text: String): List[Insight] =
    val arr = ujson.read(text.trim).arr
    arr.map(j => Insight(insight = j("insight").str, `type` = j("type").str)).toList
