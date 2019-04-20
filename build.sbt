name := "quicks-registry"

version := "0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.8"

libraryDependencies += guice
libraryDependencies += jdbc
libraryDependencies += evolutions

libraryDependencies += "org.mindrot" % "jbcrypt" % "0.4"
libraryDependencies += "com.nimbusds" % "nimbus-jose-jwt" % "6.0"

libraryDependencies += "org.postgresql" % "postgresql" % "42.2.5"