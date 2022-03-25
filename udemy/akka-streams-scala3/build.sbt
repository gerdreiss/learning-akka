val scala3Version = "3.1.1"

lazy val root = project
  .in(file("."))
  .settings(
    name                                       := "akka-streams-scala3",
    version                                    := "0.1.0-SNAPSHOT",
    scalaVersion                               := scala3Version,
    libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.19"
  )
