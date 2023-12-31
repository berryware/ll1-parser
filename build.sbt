ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.0"

lazy val root = (project in file("."))
  .settings(
    name := "ll1",
    idePackagePrefix := Some("org.exaxis.ll1"),
    libraryDependencies ++= Seq (
      "org.scalatest" %% "scalatest" % "3.2.16" % Test,
      "org.scalatestplus" %% "scalacheck-1-17" % "3.2.16.0" % Test
    )
  )
