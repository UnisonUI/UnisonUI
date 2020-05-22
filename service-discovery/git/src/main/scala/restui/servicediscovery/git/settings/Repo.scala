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
sealed trait Location

object Location {
  private val RegexPattern = "^/(.+?)/$".r

  def fromConfig(config: Config): Location =
    if (config.hasPath("location")) {
      val location = config.getString("location").trim
      if (location.isEmpty) Regex(".+")
      else RegexPattern.findFirstMatchIn(location).fold[Location](Uri(location))(m => Regex(m.group(1)))
    } else Regex(".+")

}
final case class Uri(uri: String)     extends Location
final case class Regex(regex: String) extends Location
