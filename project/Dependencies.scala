import sbt.*

object Dependencies {

  private lazy val logbackClassicVersion = "1.4.14"
  private lazy val mUnitVersion          = "1.3.4"
  private lazy val scalaTestVersion      = "3.2.19"

  private lazy val pekkoVersion                = "1.7.0"
  private lazy val pekkoPersistenceVersion     = "1.7.0"
  private lazy val pekkoPersistenceJdbcVersion = "1.3.0"

  private lazy val generexVersion = "1.0.2"

  private lazy val hikariCPVersion   = "7.1.0"
  private lazy val postgresqlVersion = "42.7.3"
  private lazy val DoobieVersion     = "1.0.0-RC12"
  private lazy val flyWayVersion     = "11.11.0"

  lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackClassicVersion

  lazy val pekkoActorTyped = "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion
  lazy val pekkoSlf4j      = "org.apache.pekko" %% "pekko-slf4j"       % pekkoVersion

  lazy val generex = "com.github.mifmif" % "generex" % generexVersion

  lazy val pekkoPersistenceTyped     = "org.apache.pekko" %% "pekko-persistence-typed"     % pekkoPersistenceVersion
  lazy val pekkoPersistenceJdbc      = "org.apache.pekko" %% "pekko-persistence-jdbc"      % pekkoPersistenceJdbcVersion
  lazy val pekkoSerializationJackson = "org.apache.pekko" %% "pekko-serialization-jackson" % pekkoVersion

  lazy val hikariCP         = "com.zaxxer"     % "HikariCP"                   % hikariCPVersion
  lazy val postgresql       = "org.postgresql" % "postgresql"                 % postgresqlVersion
  lazy val flywayCore       = "org.flywaydb"   % "flyway-core"                % flyWayVersion
  lazy val flywayPostgresql = "org.flywaydb"   % "flyway-database-postgresql" % flyWayVersion

  // Testing
  lazy val pekkoActorTestkitTyped  = "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion     % Test
  lazy val pekkoPersistenceTestkit = "org.apache.pekko" %% "pekko-persistence-testkit" % pekkoVersion     % Test
  lazy val scalatest               = "org.scalatest"    %% "scalatest"                 % scalaTestVersion % Test
  lazy val munit                   = "org.scalameta"    %% "munit"                     % mUnitVersion     % Test
}
