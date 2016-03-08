name := """playscala1"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  javaEbean,
  cache,
  ws,
  filters,
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc41",
  "io.argonaut" %% "argonaut" % "6.0.4",
  "com.bionicspirit" %% "shade" % "1.6.0",
  "com.github.nscala-time"  %%  "nscala-time" %  "2.10.0"
)

resolvers ++= Seq(
   "Spy" at "http://files.couchbase.com/maven2/"
)
