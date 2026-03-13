import com.typesafe.config.ConfigFactory
import db.Database

object LuxVita extends cask.Main:

  private val conf = ConfigFactory.load()

  Database.init(
    jdbcUrl  = conf.getString("db.url"),
    user     = conf.getString("db.user"),
    password = conf.getString("db.password"),
  )

  override val port: Int = conf.getInt("server.port")
  override val host: String = "0.0.0.0"

  override def allRoutes = Seq(
    routes.HealthRoutes,
    routes.AuthRoutes,
    routes.MealRoutes,
    routes.ActivityRoutes,
    routes.WeightRoutes,
    routes.ProfileRoutes,
    routes.InsightRoutes,
    routes.TrendsRoutes,
  )
