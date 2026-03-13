package routes

import middleware.AuthMiddleware

trait BaseRoutes extends cask.Routes:

  protected def ok(body: ujson.Value, status: Int = 200): cask.Response[String] =
    cask.Response(
      ujson.write(body),
      statusCode = status,
      headers = Seq("Content-Type" -> "application/json"),
    )

  protected def err(code: String, msg: String, status: Int): cask.Response[String] =
    cask.Response(
      ujson.write(ujson.Obj("code" -> code, "message" -> msg)),
      statusCode = status,
      headers = Seq("Content-Type" -> "application/json"),
    )

  protected def withAuth(req: cask.Request)(f: String => cask.Response[String]): cask.Response[String] =
    AuthMiddleware.extractUserId(req) match
      case Left(msg)     => err("UNAUTHORIZED", msg, 401)
      case Right(userId) => f(userId)
