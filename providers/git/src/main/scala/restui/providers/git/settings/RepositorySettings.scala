package restui.providers.git.settings

import java.{util => ju}

import scala.jdk.CollectionConverters._

import com.typesafe.config.{Config, ConfigFactory}

final case class RepositorySettings(location: Location, branch: Option[String] = None, specificationPaths: List[String] = Nil)

object RepositorySettings {

  def getListOfRepositories(config: Config, path: String): List[RepositorySettings] =
    if (config.hasPath(path))
      config.getAnyRefList(path).asScala.toList.map {
        case location: String          => RepositorySettings(Location.fromString(location))
        case config: ju.Map[String, _] => fromConfig(ConfigFactory.parseMap(config))
      }
    else Nil

  def fromConfig(config: Config): RepositorySettings = {
    val location = Location.fromConfig(config)
    val branch =
      if (config.hasPath("branch")) Some(config.getString("branch"))
      else None
    val specificationPaths =
      if (config.hasPath("specification-paths")) config.getStringList("specification-paths").asScala.toList
      else Nil
    RepositorySettings(location, branch, specificationPaths)
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
    fromString(location)
  }
  def fromString(location: String): Location =
    if (location.isEmpty) Regex(DefaultRegex)
    else RegexPattern.findFirstMatchIn(location).fold[Location](Uri(location))(m => Regex(m.group(1)))

  final case class Uri(uri: String) extends Location {
    override def isMatching(input: String): Boolean = uri == input
  }

  final case class Regex(regex: String) extends Location {
    private val pattern                             = regex.r
    override def isMatching(input: String): Boolean = pattern.matches(input)
  }
}
