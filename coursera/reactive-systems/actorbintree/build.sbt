course := "reactive"
assignment := "actorbintree"

testFrameworks += new TestFramework("munit.Framework")
Test / parallelExecution := false

val akkaVersion = "2.6.15"

scalaVersion := "3.0.1"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked"
)

libraryDependencies ++= Seq(
  ("com.typesafe.akka" %% "akka-actor" % akkaVersion).cross(CrossVersion.for3Use2_13),
  ("com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test).cross(CrossVersion.for3Use2_13),
  "org.scalameta" %% "munit" % "0.7.27" % Test
)
