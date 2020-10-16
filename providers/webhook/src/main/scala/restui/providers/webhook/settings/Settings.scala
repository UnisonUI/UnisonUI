package restui.providers.webhook.settings

import com.typesafe.config.Config

final case class Settings(interface: String,
                          port: Int,
                          selfSpecification: Boolean)

object Settings {
  private val Namespace = "restui.provider.webhook"
  def from(config: Config): Settings = {
    val namespaceConfig   = config.getConfig(Namespace)
    val port              = namespaceConfig.getInt("port")
    val interface         = namespaceConfig.getString("interface")
    val selfSpecification = namespaceConfig.getBoolean("self-specification")
    Settings(interface, port, selfSpecification)
  }
}
