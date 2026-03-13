package middleware

import cask.Request
import services.AuthService

object AuthMiddleware:

  /** Extract and validate the Bearer token from the Authorization header.
    * Returns the userId (JWT subject) or throws an unauthorized response.
    */
  def extractUserId(request: Request): Either[String, String] =
    request.headers.get("authorization")
      .flatMap(_.headOption)
      .filter(_.startsWith("Bearer "))
      .map(_.stripPrefix("Bearer ").trim)
      .flatMap(AuthService.validateToken)
      .toRight("Missing or invalid Authorization header")
