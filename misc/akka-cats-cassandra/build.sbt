lazy val akkaHttpVersion          = "10.2.9"
lazy val akkaHttpCirceVersion     = "1.39.2"
lazy val akkaVersion              = "2.6.19"
lazy val akkaPersistenceCassandra = "1.0.5"
lazy val circeVersion             = "0.14.1"
lazy val javaDriverVersion        = "4.14.0"
lazy val logbackClassicVersion    = "1.2.11"
lazy val scalaTestVersion         = "3.2.11"

lazy val root = project
  .in(file("."))
  .settings(
    name         := "akka-cats-cassandra",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := "2.13.8",
    libraryDependencies ++= Seq(
      // akka
      "com.typesafe.akka" %% "akka-actor-typed"           % akkaVersion,
      "com.typesafe.akka" %% "akka-coordination"          % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster"               % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"                % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-typed"     % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-cassandra" % akkaPersistenceCassandra,
      "com.typesafe.akka" %% "akka-http"                  % akkaHttpVersion,
      "de.heikoseeberger" %% "akka-http-circe"            % akkaHttpCirceVersion,
      // circe
      "io.circe" %% "circe-core"    % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser"  % circeVersion,
      // misc
      "ch.qos.logback" % "logback-classic" % logbackClassicVersion,
      "com.datastax.oss" % "java-driver-core" % javaDriverVersion, // See https://github.com/akka/alpakka/issues/2556
      "com.esri.geometry" % "esri-geometry-api" % "2.2.4",
      // test
      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion  % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion      % Test,
      "org.scalatest"     %% "scalatest"                % scalaTestVersion % Test
    )
  )
