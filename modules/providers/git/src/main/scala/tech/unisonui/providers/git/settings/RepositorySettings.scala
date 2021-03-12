package tech.unisonui.providers.git.settings

import java.{util => ju}

import cats.syntax.option._
import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters._

final case class RepositorySettings(location: Location,
                                    branch: Option[String] = None)

object RepositorySettings {

  def getListOfRepositories(config: Config,
                            path: String): List[RepositorySettings] =
    if (config.hasPath(path))
      config.getAnyRefList(path).asScala.toList.map {
        case location: String =>
          RepositorySettings(Location.fromString(location))
        case config: ju.Map[String, _] =>
          fromConfig(ConfigFactory.parseMap(config))
      }
    else Nil

  def fromConfig(config: Config): RepositorySettings = {
    val location = Location.fromConfig(config)
    val branch =
      if (config.hasPath("branch")) config.getString("branch").some
      else None
    RepositorySettings(location, branch)
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
    else
      RegexPattern
        .findFirstMatchIn(location)
        .fold[Location](Uri(location))(m => Regex(m.group(1)))

  final case class Uri(uri: String) extends Location {
    override def isMatching(input: String): Boolean = uri == input
  }

  final case class Regex(regex: String) extends Location {
    private val pattern                             = regex.r
    override def isMatching(input: String): Boolean = pattern.matches(input)
  }
}
