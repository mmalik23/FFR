name := "FFR"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.9.9",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "com.github.melrief" %% "purecsv" % "0.1.1",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe" % "config" % "1.3.3",
)