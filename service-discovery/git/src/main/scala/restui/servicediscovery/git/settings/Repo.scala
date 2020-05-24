package restui.servicediscovery.git.settings

import java.{util => ju}

import scala.jdk.CollectionConverters._

import com.typesafe.config.{Config, ConfigFactory}

final case class Repo(location: Location, swaggerPaths: List[String] = Nil)

object Repo {

  def getListOfRepos(config: Config, path: String): List[Repo] =
    if (config.hasPath(path))
      config.getAnyRefList(path).asScala.toList.map {
        case location: String          => Repo(Uri(location))
        case config: ju.Map[String, _] => fromConfig(ConfigFactory.parseMap(config))
      }
    else Nil

  def fromConfig(config: Config): Repo = {
    val location = Location.fromConfig(config)
    val swaggerPaths =
      if (config.hasPath("swagger-paths")) config.getStringList("swagger-paths").asScala.toList
      else Nil
    Repo(location, swaggerPaths)
  }
}
sealed trait Location {
  def isMatching(input: String): Boolean
}

object Location {
  private val RegexPattern = "^/(.+?)/$".r
  private val DefaultRegex = ".+"

  def fromConfig(config: Config): Location = {
    val location =
      if (config.hasPath("location")) config.getString("location").trim
      else ""

    if (location.isEmpty) Regex(DefaultRegex)
    else RegexPattern.findFirstMatchIn(location).fold[Location](Uri(location))(m => Regex(m.group(1)))
  }

}
final case class Uri(uri: String) extends Location {
  override def isMatching(input: String): Boolean = uri == input
}
final case class Regex(regex: String) extends Location {
  private val pattern                             = regex.r
  override def isMatching(input: String): Boolean = pattern.matches(input)
}
