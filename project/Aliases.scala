import sbt.addCommandAlias

object Aliases {
  val aliases = scalafmtAliases ++
    scalafixAliases ++
    addCommandAlias("coverage", ";coverageOn;test;coverageOff;coverageReport")

  private def scalafmtAliases =
    addCommandAlias("checkFmt", "; scalafmtCheckAll") ++
      addCommandAlias("runFmt", "; scalafmtAll; scalafmtSbt")

  private def scalafixAliases =
    addCommandAlias("checkFix",
                    ";scalafix --check; scalafix --check OrganizeImports; test:scalafix --check; test:scalafix --check OrganizeImports") ++
      addCommandAlias("runFix", ";scalafix; scalafix OrganizeImports; test:scalafix; test:scalafix OrganizeImports")
}
