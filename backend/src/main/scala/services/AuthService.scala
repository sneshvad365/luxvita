package services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.mindrot.jbcrypt.BCrypt
import java.util.Date
import java.time.Instant
import java.time.temporal.ChronoUnit

object AuthService:

  private val conf       = com.typesafe.config.ConfigFactory.load()
  private val jwtSecret  = conf.getString("jwt.secret")
  private val expiryDays = conf.getInt("jwt.expiry-days")
  private val algorithm  = Algorithm.HMAC256(jwtSecret)
  private val verifier   = JWT.require(algorithm).build()

  def hashPassword(password: String): String =
    BCrypt.hashpw(password, BCrypt.gensalt(12))

  def checkPassword(plain: String, hash: String): Boolean =
    BCrypt.checkpw(plain, hash)

  def createToken(userId: String): String =
    JWT.create()
      .withSubject(userId)
      .withIssuedAt(Date.from(Instant.now()))
      .withExpiresAt(Date.from(Instant.now().plus(expiryDays, ChronoUnit.DAYS)))
      .sign(algorithm)

  /** Returns userId if the token is valid, None otherwise. */
  def validateToken(token: String): Option[String] =
    try
      val decoded = verifier.verify(token)
      Some(decoded.getSubject)
    catch case _: Exception => None
