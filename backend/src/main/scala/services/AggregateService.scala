package services

import db.Database
import models.{Macros, ParsedActivity, UserProfile}
import java.sql.ResultSet

object AggregateService:

  // ---------------------------------------------------------------------------
  // Profile
  // ---------------------------------------------------------------------------

  def getProfile(userId: String): Option[UserProfile] =
    Database.withConnection { conn =>
      val st = conn.prepareStatement(
        """SELECT id, user_id, bio, goal,
          |  target_kcal, target_protein_g, target_carbs_g, target_fat_g,
          |  target_fiber_g, target_water_l, base_weight_kg, goal_weight_kg,
          |  updated_at
          |FROM user_profile WHERE user_id = ?::uuid""".stripMargin
      )
      st.setString(1, userId)
      val rs = st.executeQuery()
      if rs.next() then Some(profileFromRs(rs)) else None
    }

  def getProfileOrDefault(userId: String): UserProfile =
    getProfile(userId).getOrElse(
      UserProfile(
        id             = "",
        userId         = userId,
        bio            = None,
        goal           = "maintenance",
        targetKcal     = 2000,
        targetProteinG = 150,
        targetCarbsG   = 200,
        targetFatG     = 70,
        targetFiberG   = 25,
        targetWaterL   = 2.5,
        baseWeightKg   = None,
        goalWeightKg   = None,
        updatedAt      = "",
      )
    )

  // ---------------------------------------------------------------------------
  // Today macros
  // ---------------------------------------------------------------------------

  def getMacrosForDate(userId: String, date: String): Macros =
    Database.withConnection { conn =>
      val st = conn.prepareStatement(
        """SELECT COALESCE(SUM(kcal), 0)::int          AS kcal,
          |       COALESCE(SUM(protein_g), 0)::float8   AS protein_g,
          |       COALESCE(SUM(carbs_g), 0)::float8     AS carbs_g,
          |       COALESCE(SUM(fat_g), 0)::float8       AS fat_g,
          |       COALESCE(SUM(fiber_g), 0)::float8     AS fiber_g
          |FROM meals
          |WHERE user_id = ?::uuid
          |  AND logged_at >= ?::date
          |  AND logged_at <  ?::date + INTERVAL '1 day'""".stripMargin
      )
      st.setString(1, userId)
      st.setString(2, date)
      st.setString(3, date)
      val rs = st.executeQuery()
      rs.next()
      Macros(
        kcal     = rs.getInt("kcal"),
        proteinG = rs.getDouble("protein_g"),
        carbsG   = rs.getDouble("carbs_g"),
        fatG     = rs.getDouble("fat_g"),
        fiberG   = rs.getDouble("fiber_g"),
      )
    }

  def getActivityStringForDate(userId: String, date: String): String =
    Database.withConnection { conn =>
      val st = conn.prepareStatement(
        """SELECT entry FROM activity_logs
          |WHERE user_id = ?::uuid
          |  AND logged_at >= ?::date
          |  AND logged_at <  ?::date + INTERVAL '1 day'
          |ORDER BY logged_at ASC""".stripMargin
      )
      st.setString(1, userId)
      st.setString(2, date)
      st.setString(3, date)
      val rs = st.executeQuery()
      val buf = scala.collection.mutable.ArrayBuffer[String]()
      while rs.next() do buf += rs.getString("entry")
      buf.mkString("; ")
    }

  def getTodayMacros(userId: String): Macros =
    Database.withConnection { conn =>
      val st = conn.prepareStatement(
        """SELECT COALESCE(SUM(kcal), 0)::int          AS kcal,
          |       COALESCE(SUM(protein_g), 0)::float8   AS protein_g,
          |       COALESCE(SUM(carbs_g), 0)::float8     AS carbs_g,
          |       COALESCE(SUM(fat_g), 0)::float8       AS fat_g,
          |       COALESCE(SUM(fiber_g), 0)::float8     AS fiber_g
          |FROM meals
          |WHERE user_id = ?::uuid
          |  AND logged_at >= date_trunc('day', now() AT TIME ZONE 'UTC')""".stripMargin
      )
      st.setString(1, userId)
      val rs = st.executeQuery()
      rs.next()
      Macros(
        kcal     = rs.getInt("kcal"),
        proteinG = rs.getDouble("protein_g"),
        carbsG   = rs.getDouble("carbs_g"),
        fatG     = rs.getDouble("fat_g"),
        fiberG   = rs.getDouble("fiber_g"),
      )
    }

  // ---------------------------------------------------------------------------
  // N-day average macros
  // ---------------------------------------------------------------------------

  def getNDayAvgMacros(userId: String, days: Int): Macros =
    Database.withConnection { conn =>
      val st = conn.prepareStatement(
        """WITH daily AS (
          |  SELECT date_trunc('day', logged_at AT TIME ZONE 'UTC') AS day,
          |         COALESCE(SUM(kcal), 0)::float8        AS kcal,
          |         COALESCE(SUM(protein_g), 0)::float8   AS protein_g,
          |         COALESCE(SUM(carbs_g), 0)::float8     AS carbs_g,
          |         COALESCE(SUM(fat_g), 0)::float8       AS fat_g,
          |         COALESCE(SUM(fiber_g), 0)::float8     AS fiber_g
          |  FROM meals
          |  WHERE user_id = ?::uuid
          |    AND logged_at >= now() - ? * INTERVAL '1 day'
          |  GROUP BY day
          |)
          |SELECT COALESCE(AVG(kcal), 0)::float8      AS kcal,
          |       COALESCE(AVG(protein_g), 0)::float8 AS protein_g,
          |       COALESCE(AVG(carbs_g), 0)::float8   AS carbs_g,
          |       COALESCE(AVG(fat_g), 0)::float8     AS fat_g,
          |       COALESCE(AVG(fiber_g), 0)::float8   AS fiber_g
          |FROM daily""".stripMargin
      )
      st.setString(1, userId)
      st.setInt(2, days)
      val rs = st.executeQuery()
      rs.next()
      Macros(
        kcal     = rs.getDouble("kcal").toInt,
        proteinG = rs.getDouble("protein_g"),
        carbsG   = rs.getDouble("carbs_g"),
        fatG     = rs.getDouble("fat_g"),
        fiberG   = rs.getDouble("fiber_g"),
      )
    }

  // ---------------------------------------------------------------------------
  // Today activity string
  // ---------------------------------------------------------------------------

  def getTodayActivityString(userId: String): String =
    Database.withConnection { conn =>
      val st = conn.prepareStatement(
        """SELECT entry FROM activity_logs
          |WHERE user_id = ?::uuid
          |  AND logged_at >= date_trunc('day', now() AT TIME ZONE 'UTC')
          |ORDER BY logged_at ASC""".stripMargin
      )
      st.setString(1, userId)
      val rs = st.executeQuery()
      val buf = scala.collection.mutable.ArrayBuffer[String]()
      while rs.next() do buf += rs.getString("entry")
      buf.mkString("; ")
    }

  // ---------------------------------------------------------------------------
  // Latest weight
  // ---------------------------------------------------------------------------

  def getMedicalContext(userId: String): String =
    Database.withConnection { conn =>
      val st = conn.prepareStatement(
        """SELECT title, content, created_at
          |FROM medical_records
          |WHERE user_id = ?::uuid
          |ORDER BY created_at DESC
          |LIMIT 5""".stripMargin
      )
      st.setString(1, userId)
      val rs  = st.executeQuery()
      val buf = scala.collection.mutable.ArrayBuffer[String]()
      while rs.next() do
        val date = rs.getString("created_at").take(10)
        buf += s"[${date}] ${rs.getString("title")}:\n${rs.getString("content")}"
      buf.mkString("\n\n---\n\n")
    }

  def getLatestWeightKg(userId: String): Option[Double] =
    Database.withConnection { conn =>
      val st = conn.prepareStatement(
        """SELECT weight_kg::float8 FROM weight_logs
          |WHERE user_id = ?::uuid
          |ORDER BY logged_at DESC LIMIT 1""".stripMargin
      )
      st.setString(1, userId)
      val rs = st.executeQuery()
      if rs.next() then
        val v = rs.getDouble("weight_kg")
        if rs.wasNull() then None else Some(v)
      else None
    }

  // ---------------------------------------------------------------------------
  // Calorie adjustment based on parsed activity
  // ---------------------------------------------------------------------------

  def adjustedCalorieTarget(baseKcal: Int, parsed: Option[ParsedActivity]): Int =
    parsed match
      case None => baseKcal
      case Some(p) =>
        val isHigh = p.intensity.exists(_ == "high")
        val dur    = p.durationMin.getOrElse(0)
        if dur == 0 then baseKcal
        else if dur > 60 || isHigh then baseKcal + 400
        else if dur >= 30 then baseKcal + 250
        else baseKcal + 125

  // ---------------------------------------------------------------------------
  // Pattern notes
  // ---------------------------------------------------------------------------

  def patternNotes(userId: String, profile: UserProfile): String =
    Database.withConnection { conn =>
      // Count days in last 7 where protein target was missed
      val st1 = conn.prepareStatement(
        """WITH daily AS (
          |  SELECT date_trunc('day', logged_at AT TIME ZONE 'UTC') AS day,
          |         COALESCE(SUM(protein_g), 0)::float8 AS protein_g
          |  FROM meals
          |  WHERE user_id = ?::uuid
          |    AND logged_at >= now() - INTERVAL '7 days'
          |  GROUP BY day
          |)
          |SELECT COUNT(*) AS missed FROM daily WHERE protein_g < ?""".stripMargin
      )
      st1.setString(1, userId)
      st1.setDouble(2, profile.targetProteinG.toDouble)
      val rs1 = st1.executeQuery()
      rs1.next()
      val proteinMissed = rs1.getInt("missed")

      // Calorie trend over last 7 days (slope via linear regression approximation)
      val st2 = conn.prepareStatement(
        """WITH daily AS (
          |  SELECT date_trunc('day', logged_at AT TIME ZONE 'UTC') AS day,
          |         COALESCE(SUM(kcal), 0)::float8 AS kcal,
          |         ROW_NUMBER() OVER (ORDER BY date_trunc('day', logged_at AT TIME ZONE 'UTC')) AS rn
          |  FROM meals
          |  WHERE user_id = ?::uuid
          |    AND logged_at >= now() - INTERVAL '7 days'
          |  GROUP BY day
          |)
          |SELECT COALESCE(AVG(kcal), 0)::float8 AS avg_kcal,
          |       COALESCE(
          |         (SUM(rn * kcal) - SUM(rn) * AVG(kcal)) /
          |         NULLIF(SUM(rn * rn) - COUNT(*) * AVG(rn) * AVG(rn), 0),
          |         0
          |       )::float8 AS slope
          |FROM daily""".stripMargin
      )
      st2.setString(1, userId)
      val rs2 = st2.executeQuery()
      rs2.next()
      val slope = rs2.getDouble("slope")

      val trendStr =
        if slope > 50 then s"calories trending +${slope.toInt} kcal/day"
        else if slope < -50 then s"calories trending ${slope.toInt} kcal/day"
        else "calories roughly stable"

      s"protein missed ${proteinMissed}/7 days; $trendStr"
    }

  // ---------------------------------------------------------------------------
  // Consistency scores
  // ---------------------------------------------------------------------------

  def consistencyScores(userId: String, profile: UserProfile): (Double, Double, Double) =
    Database.withConnection { conn =>
      val st = conn.prepareStatement(
        """WITH daily AS (
          |  SELECT date_trunc('day', logged_at AT TIME ZONE 'UTC') AS day,
          |         COALESCE(SUM(kcal), 0)::float8      AS kcal,
          |         COALESCE(SUM(protein_g), 0)::float8 AS protein_g,
          |         COALESCE(SUM(fiber_g), 0)::float8   AS fiber_g
          |  FROM meals
          |  WHERE user_id = ?::uuid
          |    AND logged_at >= now() - INTERVAL '30 days'
          |  GROUP BY day
          |)
          |SELECT
          |  COUNT(*) AS total_days,
          |  SUM(CASE WHEN kcal    >= ? * 0.85 AND kcal    <= ? * 1.15 THEN 1 ELSE 0 END) AS kcal_ok,
          |  SUM(CASE WHEN protein_g >= ? * 0.85                        THEN 1 ELSE 0 END) AS protein_ok,
          |  SUM(CASE WHEN fiber_g   >= ? * 0.85                        THEN 1 ELSE 0 END) AS fiber_ok
          |FROM daily""".stripMargin
      )
      st.setString(1, userId)
      st.setDouble(2, profile.targetKcal.toDouble)
      st.setDouble(3, profile.targetKcal.toDouble)
      st.setDouble(4, profile.targetProteinG.toDouble)
      st.setDouble(5, profile.targetFiberG.toDouble)
      val rs = st.executeQuery()
      rs.next()
      val total = rs.getInt("total_days")
      if total == 0 then (0.0, 0.0, 0.0)
      else
        val kcalOk    = rs.getInt("kcal_ok")
        val proteinOk = rs.getInt("protein_ok")
        val fiberOk   = rs.getInt("fiber_ok")
        (kcalOk.toDouble / total, proteinOk.toDouble / total, fiberOk.toDouble / total)
    }

  // ---------------------------------------------------------------------------
  // Private helpers
  // ---------------------------------------------------------------------------

  private def profileFromRs(rs: ResultSet): UserProfile =
    val id             = rs.getString("id")
    val userId         = rs.getString("user_id")
    val bio            = rs.getString("bio")
    val bioOpt         = if rs.wasNull() then None else Some(bio)
    val goal           = rs.getString("goal")
    val targetKcal     = rs.getInt("target_kcal")
    val targetProteinG = rs.getInt("target_protein_g")
    val targetCarbsG   = rs.getInt("target_carbs_g")
    val targetFatG     = rs.getInt("target_fat_g")
    val targetFiberG   = rs.getInt("target_fiber_g")
    val targetWaterL   = rs.getDouble("target_water_l")
    val baseWeightRaw  = rs.getDouble("base_weight_kg")
    val baseWeightOpt  = if rs.wasNull() then None else Some(baseWeightRaw)
    val goalWeightRaw  = rs.getDouble("goal_weight_kg")
    val goalWeightOpt  = if rs.wasNull() then None else Some(goalWeightRaw)
    val updatedAt      = rs.getString("updated_at")
    UserProfile(
      id             = id,
      userId         = userId,
      bio            = bioOpt,
      goal           = goal,
      targetKcal     = targetKcal,
      targetProteinG = targetProteinG,
      targetCarbsG   = targetCarbsG,
      targetFatG     = targetFatG,
      targetFiberG   = targetFiberG,
      targetWaterL   = targetWaterL,
      baseWeightKg   = baseWeightOpt,
      goalWeightKg   = goalWeightOpt,
      updatedAt      = updatedAt,
    )
