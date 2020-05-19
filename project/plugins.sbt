externalResolvers := Seq(
  Resolver.sbtPluginRepo("releases")
)
addSbtPlugin("com.typesafe.sbt"  % "sbt-native-packager"  % "1.7.0")
addSbtPlugin("com.eed3si9n"      % "sbt-assembly"         % "0.14.10")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"         % "2.3.4")
addSbtPlugin("com.timushev.sbt"  % "sbt-updates"          % "0.5.0")
addSbtPlugin("net.virtual-void"  % "sbt-dependency-graph" % "0.10.0-RC1")
addSbtPlugin("ch.epfl.scala"     % "sbt-scalafix"         % "0.9.15-1")
addSbtPlugin("com.github.mwz"    % "sbt-sonar"            % "2.1.0")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"        % "1.6.1")
addSbtPlugin("com.github.gseitz" % "sbt-release"          % "1.0.13")
// to prevent conflict with logback over slf4j
excludeDependencies ++= Seq(
  ExclusionRule("org.slf4j", "slf4j-log4j12")
)
