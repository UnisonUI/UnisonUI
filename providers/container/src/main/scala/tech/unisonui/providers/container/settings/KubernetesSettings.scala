package tech.unisonui.providers.container.settings

import scala.concurrent.duration.FiniteDuration
final case class KubernetesSettings(pollingInterval: FiniteDuration)
