val scala3Version = "3.3.7"

//Automatically reload the build when source changes are detected by setting `Global / onChangedBuildSource := ReloadOnSourceChanges`.
Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = project
  .in(file("."))
  .settings(
    name         := "syllabol",
    organization := "indigonet",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version
  )
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.logbackClassic,
      Dependencies.pekkoActorTyped,
      Dependencies.pekkoSlf4j,
      Dependencies.generex,
      Dependencies.pekkoPersistenceTyped,
      Dependencies.pekkoPersistenceJdbc,
      Dependencies.pekkoSerializationJackson,
      Dependencies.postgresql,
      Dependencies.hikariCP,
      Dependencies.flywayCore,
      Dependencies.flywayPostgresql
    )
  )
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.munit,
      Dependencies.scalatest,
      Dependencies.pekkoActorTestkitTyped,
      Dependencies.pekkoPersistenceTestkit
    )

//    excludeDependencies ++= Seq(
//      "org.slf4j" % "slf4j-api",
//      "commons-logging" % "commons-logging",
//    ),
  )
  .settings(
    excludeDependencies ++= Seq(
//      "org.slf4j" % "slf4j-api",
    )
  )
