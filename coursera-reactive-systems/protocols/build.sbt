course := "reactive"
assignment := "protocols"

scalaVersion := "3.0.0"

val akkaVersion = "2.6.9"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
).map(_.cross(CrossVersion.for3Use2_13))

libraryDependencies += "org.scalameta" %% "munit" % "0.7.26" % Test

testFrameworks += new TestFramework("munit.Framework")
Test / parallelExecution := false

