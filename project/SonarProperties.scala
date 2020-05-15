object SonarProperties {

  def sonarProp(moduleName: String) = Map(
    "sonar.sources"                    -> s"$moduleName/src/main/scala",
    "sonar.tests"                      -> s"$moduleName/src/test/scala",
    "sonar.junit.reportPaths"          -> s"$moduleName/target/test-reports",
    "sonar.scala.scapegoat.disable"    -> "true",
    "sonar.scala.scoverage.reportPath" -> s"$moduleName/scala-2.13/scoverage-report/scoverage.xml"
  )
}
