name := """reactive-quizzo"""

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.4",
  "org.scalatestplus" %% "play" % "1.2.0" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.4" % "test",
  "io.spray" %% "spray-client" % "1.3.2" % "test",
  jdbc,
  anorm,
  cache,
  ws
)

scalacOptions ++= Seq("-feature")
