package routes

import db.Database
import services.AuthService

object AuthRoutes extends BaseRoutes:

  @cask.post("/api/auth/register")
  def register(request: cask.Request): cask.Response[String] =
    try
      val body     = ujson.read(request.text())
      val email    = body("email").str.trim.toLowerCase
      val password = body("password").str

      if email.isEmpty || password.length < 8 then
        err("INVALID_INPUT", "Email required and password must be at least 8 characters", 400)
      else
        val hash   = AuthService.hashPassword(password)
        val userId = Database.withConnection { conn =>
          val st = conn.prepareStatement("INSERT INTO users(email, password_hash) VALUES (?, ?) RETURNING id")
          st.setString(1, email)
          st.setString(2, hash)
          val rs = st.executeQuery()
          rs.next()
          rs.getString("id")
        }

        val token = AuthService.createToken(userId)
        ok(ujson.Obj("token" -> token, "userId" -> userId), status = 201)

    catch
      case e: org.postgresql.util.PSQLException if e.getSQLState == "23505" =>
        err("EMAIL_TAKEN", "Email already registered", 409)
      case e: Exception =>
        err("SERVER_ERROR", e.getMessage, 500)

  @cask.post("/api/auth/login")
  def login(request: cask.Request): cask.Response[String] =
    try
      val body     = ujson.read(request.text())
      val email    = body("email").str.trim.toLowerCase
      val password = body("password").str

      val userOpt = Database.withConnection { conn =>
        val st = conn.prepareStatement("SELECT id, password_hash FROM users WHERE email = ?")
        st.setString(1, email)
        val rs = st.executeQuery()
        if rs.next() then Some((rs.getString("id"), rs.getString("password_hash")))
        else None
      }

      userOpt match
        case None =>
          err("INVALID_CREDENTIALS", "Invalid email or password", 401)
        case Some((userId, hash)) if !AuthService.checkPassword(password, hash) =>
          err("INVALID_CREDENTIALS", "Invalid email or password", 401)
        case Some((userId, _)) =>
          val token = AuthService.createToken(userId)
          ok(ujson.Obj("token" -> token, "userId" -> userId))

    catch
      case e: Exception =>
        err("SERVER_ERROR", e.getMessage, 500)

  initialize()
