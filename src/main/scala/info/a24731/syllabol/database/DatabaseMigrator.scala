package info.a24731.syllabol.database

import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory

object DatabaseMigrator:
  private val logger = LoggerFactory.getLogger(getClass)

  def migrate(url: String, user: String, pass: String): Unit =
    logger.info(s"Applying Flyway migrations to $url...")
    val flyway = Flyway
      .configure()
      .dataSource(url, user, pass)
      .locations("classpath:db/migration")
      .load()

    val applied = flyway.migrate()
    logger.info(s"Flyway migration finished. Applied ${applied.migrationsExecuted} migrations.")
