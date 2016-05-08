name := """playscala1"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers += "Spy" at "http://files.couchbase.com/maven2/"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// playEbeanDebugLevel := 1

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  filters,
  specs2 % Test,
  "org.postgresql" % "postgresql" % "9.4.1207",
  "com.bionicspirit" %% "shade" % "1.6.0",
  "com.github.nscala-time"  %%  "nscala-time" %  "2.10.0",
  "org.apache.poi" % "poi" % "3.12",
  "org.apache.poi" % "poi-ooxml" % "3.12"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala, PlayEbean)

routesGenerator := InjectedRoutesGenerator