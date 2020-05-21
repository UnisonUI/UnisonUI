object ScalacOptions {
  val options = Seq(
    "-encoding",
    "UTF-8",
    "-deprecation",
    "-unchecked",
    "-Yrangepos",
    "-Ywarn-dead-code",
    "-Ywarn-unused",
    "-Wunused:imports",
    "-language:implicitConversions",
    "-language:higherKinds",
    "-target:11",
    "-feature"
  )
}
