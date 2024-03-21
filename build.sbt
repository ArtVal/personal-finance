import sbt.Keys.version

lazy val root = (project in file("."))
  .settings(
    name:="Personal Finance",
    version := "1.0",
    scalaVersion := "2.13.13",
    resolvers ++= Seq(
      Resolver.mavenLocal,
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases",
    ),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.0.21",
      "dev.zio" %% "zio-config" % "4.0.1",
      "dev.zio" %% "zio-config-typesafe" % "4.0.1",
      "dev.zio" %% "zio-http" % "3.0.0-RC3",
      "dev.zio" %% "zio-crypto" % "0.0.0+249-d7572168-SNAPSHOT",
      "io.getquill"  %% "quill-jdbc-zio" % "4.8.3",
      "org.postgresql" % "postgresql" % "42.7.2",
      "org.liquibase" % "liquibase-core" % "4.26.0",
      "com.github.jwt-scala" %% "jwt-zio-json" % "10.0.0"),
      Compile/run/mainClass := Some("App")
  )
