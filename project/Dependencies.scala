import sbt.*

object Dependencies {

  private lazy val logbackClassicVersion = "1.4.14"
  private lazy val mUnitVersion = "1.3.4"
  private lazy val scalaTestVersion = "3.2.19"

  lazy val pekkoVersion = "1.7.0"

  lazy val generexVersion = "1.0.2"


  lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackClassicVersion

  lazy val pekkoActorTyped = "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion
  lazy val pekkoSlf4j = "org.apache.pekko" %% "pekko-slf4j" % pekkoVersion

  lazy val generex = "com.github.mifmif" % "generex" % generexVersion

  lazy val pekkoActorTestkitTyped =  "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion % Test
  lazy val scalatest = "org.scalatest" %% "scalatest" % scalaTestVersion % Test
  lazy val munit = "org.scalameta" %% "munit" % mUnitVersion % Test
}
