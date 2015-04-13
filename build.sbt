name := """playscala1"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  jdbc,
  javaEbean,
  cache,
  ws,
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc41",
  "io.argonaut" %% "argonaut" % "6.0.4",
  "com.bionicspirit" %% "shade" % "1.6.0"
)

resolvers += "Spy" at "http://files.couchbase.com/maven2/"
