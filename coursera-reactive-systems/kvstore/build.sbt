course := "reactive"
assignment := "kvstore"

scalaVersion := "3.0.0"

val akkaVersion = "2.6.9"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked"
)

libraryDependencies ++= Seq(
  ("com.typesafe.akka" %% "akka-actor" % akkaVersion).cross(CrossVersion.for3Use2_13),
  ("com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test).cross(CrossVersion.for3Use2_13),
  "org.scalameta" %% "munit" % "0.7.26" % Test
)

testFrameworks += new TestFramework("munit.Framework")
Test / parallelExecution := false
