import sbt.Keys.version

lazy val root = (project in file("."))
  .settings(
    name:="Personal Finance",
    version := "1.0",
    scalaVersion := "2.13.13",


    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.0.21",
      "dev.zio" %% "zio-config" % "4.0.1",
      "dev.zio" %% "zio-config-typesafe" % "4.0.1",
      "dev.zio" %% "zio-http" % "3.0.0-RC3",
      "io.getquill"  %% "quill-jdbc-zio" % "4.8.3",
      "org.postgresql" % "postgresql" % "42.7.2",
      "org.liquibase" % "liquibase-core" % "4.26.0",
      "com.github.jwt-scala" %% "jwt-zio-json" % "10.0.0"),
      Compile/run/mainClass := Some("App")
  )
