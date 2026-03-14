package db

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.flywaydb.core.Flyway
import javax.sql.DataSource

object Database:

  private var _ds: HikariDataSource = _

  def init(jdbcUrl: String, user: String, password: String): Unit =
    val cfg = HikariConfig()
    cfg.setDriverClassName("org.postgresql.Driver")
    cfg.setJdbcUrl(jdbcUrl)
    cfg.setUsername(user)
    cfg.setPassword(password)
    cfg.setMaximumPoolSize(10)
    cfg.setMinimumIdle(2)
    cfg.setConnectionTimeout(30_000)
    cfg.setIdleTimeout(600_000)
    cfg.setMaxLifetime(1_800_000)
    _ds = HikariDataSource(cfg)

    Flyway.configure()
      .dataSource(_ds)
      .locations("classpath:db/migration")
      .load()
      .migrate()

  def dataSource: DataSource =
    require(_ds != null, "Database.init() must be called before accessing dataSource")
    _ds

  def withConnection[A](f: java.sql.Connection => A): A =
    val conn = _ds.getConnection()
    try f(conn)
    finally conn.close()
