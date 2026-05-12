name := "pokedex-play"
version := "1.0-SNAPSHOT"
scalaVersion := "3.4.3"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  guice,
  ws,
  "io.spray" %% "spray-json" % "1.3.6"
)
