name := """playscala1"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, PlayJava, PlayEbean)

scalaVersion := "2.11.7"

playEbeanDebugLevel := 4

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  filters,
  specs2 % Test,
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc41",
  "io.argonaut" %% "argonaut" % "6.0.4",
  "com.bionicspirit" %% "shade" % "1.6.0",
  "com.github.nscala-time"  %%  "nscala-time" %  "2.10.0"
)

resolvers += "Spy" at "http://files.couchbase.com/maven2/"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

routesGenerator := InjectedRoutesGenerator