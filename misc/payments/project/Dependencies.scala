import sbt._

object Dependencies {
  lazy val akkaTyped = "com.typesafe.akka" %% "akka-actor-typed" % "2.5.25"
  lazy val akkaTypedTest = "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.5.25"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8"
}
