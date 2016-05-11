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
  "com.mohiva" %% "play-silhouette" % "3.0.0",
  "org.webjars" %% "webjars-play" % "2.4.0",
  "net.codingwell" %% "scala-guice" % "4.0.0",
  "net.ceedubs" %% "ficus" % "1.1.2",
  "com.adrianhurt" %% "play-bootstrap3" % "0.4.4-P24",
  "com.mohiva" %% "play-silhouette-testkit" % "3.0.0" % "test",
  "org.postgresql" % "postgresql" % "9.4.1207",
  "com.bionicspirit" %% "shade" % "1.6.0",
  "com.github.nscala-time"  %%  "nscala-time" %  "2.10.0",
  "org.apache.poi" % "poi" % "3.12",
  "org.apache.poi" % "poi-ooxml" % "3.12"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala, PlayEbean)

routesGenerator := InjectedRoutesGenerator