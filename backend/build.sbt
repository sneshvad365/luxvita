lazy val root = project
  .in(file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name         := "luxvita-backend",
    version      := "0.1.0",
    scalaVersion := "3.3.3",

    libraryDependencies ++= Seq(
      // HTTP server
      "com.lihaoyi" %% "cask"              % "0.9.4",

      // JSON
      "com.lihaoyi" %% "upickle"           % "3.3.1",

      // Database
      "com.lihaoyi" %% "scalasql"          % "0.1.19",
      "org.postgresql" % "postgresql"      % "42.7.3",
      "com.zaxxer"     % "HikariCP"        % "5.1.0",

      // Migrations
      "org.flywaydb"   % "flyway-core"     % "9.22.3",

      // Auth
      "org.mindrot" % "jbcrypt"            % "0.4",
      "com.auth0"   % "java-jwt"           % "4.4.0",

      // Config
      "com.typesafe" % "config"            % "1.4.3",

      // Logging (satisfies SLF4J used by HikariCP and Flyway)
      "org.slf4j"    % "slf4j-simple"      % "2.0.13",
    ),

    // Run in forked JVM so System.exit() works cleanly
    fork := true,

    // Assembly / packaging settings
    Compile / mainClass := Some("LuxVita"),
  )
