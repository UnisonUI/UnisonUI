externalResolvers := Seq(
  Resolver.sbtPluginRepo("releases")
)
addDependencyTreePlugin
addSbtPlugin("com.typesafe.sbt"  % "sbt-native-packager"       % "1.7.5")
addSbtPlugin("com.eed3si9n"      % "sbt-assembly"              % "0.14.10")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"              % "2.4.2")
addSbtPlugin("com.timushev.sbt"  % "sbt-updates"               % "0.5.2")
addSbtPlugin("ch.epfl.scala"     % "sbt-scalafix"              % "0.9.21")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"             % "1.6.1")
addSbtPlugin("com.github.cb372"  % "sbt-explicit-dependencies" % "0.2.13")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker"                % "1.8.0")

// to prevent conflict with logback over slf4j
excludeDependencies ++= Seq(
  ExclusionRule("org.slf4j", "slf4j-log4j12")
)
