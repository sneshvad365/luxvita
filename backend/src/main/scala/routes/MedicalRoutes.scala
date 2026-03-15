package routes

import db.Database
import services.{AggregateService, ClaudeService}

object MedicalRoutes extends BaseRoutes:

  @cask.post("/api/medical")
  def createRecord(request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val body  = ujson.read(request.text())
        val title = body("title").str.trim

        if title.isEmpty then
          err("INVALID_INPUT", "Title is required", 400)
        else
          // Detect source type and file data
          val (sourceType, fileDataOpt, fileMimeOpt) =
            if body.obj.get("pdf").exists(!_.isNull) then
              ("pdf", Some(body("pdf").str), Some("application/pdf"))
            else if body.obj.get("image").exists(!_.isNull) then
              ("image", Some(body("image").str), Some("image/jpeg"))
            else
              ("text", None, None)

          val (content, documentDate) = sourceType match
            case "pdf" | "image" =>
              val mediaType = fileMimeOpt.get
              ClaudeService.extractMedicalText(title, fileDataOpt.get, mediaType)
            case _ =>
              val c = body("content").str.trim
              if c.isEmpty then return err("INVALID_INPUT", "Content is required", 400)
              (c, None)

          val (recId, createdAt) = Database.withConnection { conn =>
            val sql = documentDate match
              case Some(_) =>
                """INSERT INTO medical_records (user_id, title, content, source_type, file_data, file_mime_type, created_at)
                  |VALUES (?::uuid, ?, ?, ?, ?, ?, ?::date)
                  |RETURNING id, created_at""".stripMargin
              case None =>
                """INSERT INTO medical_records (user_id, title, content, source_type, file_data, file_mime_type)
                  |VALUES (?::uuid, ?, ?, ?, ?, ?)
                  |RETURNING id, created_at""".stripMargin
            val st = conn.prepareStatement(sql)
            st.setString(1, userId)
            st.setString(2, title)
            st.setString(3, content)
            st.setString(4, sourceType)
            fileDataOpt match
              case Some(fd) => st.setString(5, fd)
              case None     => st.setNull(5, java.sql.Types.VARCHAR)
            fileMimeOpt match
              case Some(fm) => st.setString(6, fm)
              case None     => st.setNull(6, java.sql.Types.VARCHAR)
            documentDate.foreach(d => st.setString(7, d))
            val rs = st.executeQuery()
            rs.next()
            (rs.getString("id"), rs.getString("created_at"))
          }

          ok(ujson.Obj(
            "id"         -> recId,
            "title"      -> title,
            "sourceType" -> sourceType,
            "createdAt"  -> createdAt,
          ), status = 201)

      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  @cask.get("/api/medical")
  def listRecords(request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val records = Database.withConnection { conn =>
          val st = conn.prepareStatement(
            """SELECT id, title, source_type, created_at,
              |       (content <> 'NO_MEDICAL_CONTENT') AS has_content
              |FROM medical_records
              |WHERE user_id = ?::uuid
              |ORDER BY created_at DESC""".stripMargin
          )
          st.setString(1, userId)
          val rs  = st.executeQuery()
          val buf = scala.collection.mutable.ArrayBuffer[ujson.Obj]()
          while rs.next() do
            buf += ujson.Obj(
              "id"         -> rs.getString("id"),
              "title"      -> rs.getString("title"),
              "sourceType" -> rs.getString("source_type"),
              "createdAt"  -> rs.getString("created_at"),
              "hasContent" -> rs.getBoolean("has_content"),
            )
          buf.toList
        }
        ok(ujson.Obj("records" -> ujson.Arr(records*)))
      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  @cask.get("/api/medical/:id")
  def getRecord(id: String, request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val record = Database.withConnection { conn =>
          val st = conn.prepareStatement(
            """SELECT id, title, content, source_type, file_data, file_mime_type, created_at
              |FROM medical_records
              |WHERE id = ?::uuid AND user_id = ?::uuid""".stripMargin
          )
          st.setString(1, id)
          st.setString(2, userId)
          val rs = st.executeQuery()
          if rs.next() then
            val obj = ujson.Obj(
              "id"         -> rs.getString("id"),
              "title"      -> rs.getString("title"),
              "content"    -> rs.getString("content"),
              "sourceType" -> rs.getString("source_type"),
              "createdAt"  -> rs.getString("created_at"),
            )
            Option(rs.getString("file_data")).foreach(fd => obj("fileData") = fd)
            Option(rs.getString("file_mime_type")).foreach(fm => obj("fileMimeType") = fm)
            Some(obj)
          else None
        }
        record match
          case Some(r) => ok(r)
          case None    => err("NOT_FOUND", "Record not found", 404)
      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  @cask.put("/api/medical/:id")
  def updateRecord(id: String, request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val body    = ujson.read(request.text())
        val title   = body("title").str.trim
        val content = body("content").str.trim
        val date    = body.obj.get("date").flatMap(v => if v.isNull then None else Some(v.str)).filter(_.nonEmpty)

        if title.isEmpty || content.isEmpty then
          err("INVALID_INPUT", "Title and content are required", 400)
        else
          val (sql, params) = date match
            case Some(d) =>
              ("""UPDATE medical_records SET title = ?, content = ?, created_at = ?::date
                 |WHERE id = ?::uuid AND user_id = ?::uuid
                 |RETURNING id, created_at""".stripMargin,
               List(title, content, d, id, userId))
            case None =>
              ("""UPDATE medical_records SET title = ?, content = ?
                 |WHERE id = ?::uuid AND user_id = ?::uuid
                 |RETURNING id, created_at""".stripMargin,
               List(title, content, id, userId))

          val result = Database.withConnection { conn =>
            val st = conn.prepareStatement(sql)
            params.zipWithIndex.foreach { case (v, i) => st.setString(i + 1, v) }
            val rs = st.executeQuery()
            if rs.next() then Some(rs.getString("created_at")) else None
          }
          result match
            case Some(createdAt) => ok(ujson.Obj("id" -> id, "title" -> title, "createdAt" -> createdAt))
            case None            => err("NOT_FOUND", "Record not found", 404)
      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  @cask.delete("/api/medical/:id")
  def deleteRecord(id: String, request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val deleted = Database.withConnection { conn =>
          val st = conn.prepareStatement(
            "DELETE FROM medical_records WHERE id = ?::uuid AND user_id = ?::uuid RETURNING id"
          )
          st.setString(1, id)
          st.setString(2, userId)
          st.executeQuery().next()
        }
        if deleted then ok(ujson.Obj("id" -> id))
        else err("NOT_FOUND", "Record not found", 404)
      catch
        case e: Exception =>
          err("SERVER_ERROR", e.getMessage, 500)
    }

  @cask.post("/api/medical/insight")
  def getInsight(request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val medicalContext = AggregateService.getMedicalContext(userId)
        if medicalContext.isEmpty then
          err("NO_RECORDS", "No medical records found", 400)
        else
          val profile = AggregateService.getProfileOrDefault(userId)
          val insight = ClaudeService.medicalInsight(medicalContext, profile)
          ok(ujson.Obj("insight" -> insight))
      catch
        case e: Exception => err("SERVER_ERROR", e.getMessage, 500)
    }

  @cask.post("/api/medical/chat")
  def chat(request: cask.Request): cask.Response[String] =
    withAuth(request) { userId =>
      try
        val body     = ujson.read(request.text())
        val messages = body("messages").arr.map(m => (m("role").str, m("content").str)).toList
        if messages.isEmpty then
          err("INVALID_INPUT", "messages is required", 400)
        else
          val medicalContext = AggregateService.getMedicalContext(userId)
          val profile        = AggregateService.getProfileOrDefault(userId)
          val reply          = ClaudeService.medicalChat(medicalContext, profile, messages)
          ok(ujson.Obj("reply" -> reply))
      catch
        case e: Exception => err("SERVER_ERROR", e.getMessage, 500)
    }

  initialize()
