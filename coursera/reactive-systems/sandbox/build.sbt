val scala3Version = "3.0.1"

lazy val root = project
  .in(file("."))
  .settings(
    name         := "sandbox",
    version      := "0.1.0",
    scalaVersion := scala3Version,
    libraryDependencies ++=
      Seq(
        ("com.typesafe.akka" %% "akka-actor-typed" % "2.6.15")
          .cross(CrossVersion.for3Use2_13),
        "com.novocode"        % "junit-interface"  % "0.11" % "test"
      )
  )
