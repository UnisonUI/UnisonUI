package tech.unisonui.providers.container.settings

import cats.syntax.option._
import com.typesafe.config.Config

import scala.jdk.DurationConverters._
import scala.util.control.Exception.allCatch

final case class Settings(docker: Option[DockerSettings],
                          kubernetes: Option[KubernetesSettings],
                          labels: Labels)
// $COVERAGE-OFF$
object Settings {

  private val Namespace         = "unisonui.provider.container"
  private val OpenApiLabelsPath = "unisonui.provider.container.labels.openapi"
  private val GrpcLabelsPath    = "unisonui.provider.container.labels.grpc"

  def from(config: Config): Settings = {
    val dockerEnabled = allCatch
      .opt(config.getBoolean(s"$Namespace.docker.enabled"))
      .getOrElse(true)

    val dockerSettings =
      if (dockerEnabled)
        DockerSettings(config.getString(s"$Namespace.docker.host")).some
      else Option.empty[DockerSettings]

    val kubernetesEnabled = allCatch
      .opt(config.getBoolean(s"$Namespace.kubernetes.enabled"))
      .getOrElse(true)

    val kubernetesSettings =
      if (kubernetesEnabled)
        allCatch
          .opt(config.getDuration(s"$Namespace.kubernetes.polling-interval"))
          .map(duration => KubernetesSettings(duration.toScala))
      else Option.empty[KubernetesSettings]

    val serviceName = config.getString(s"$Namespace.labels.service-name")

    val port     = config.getString(s"$OpenApiLabelsPath.port")
    val protocol = config.getString(s"$OpenApiLabelsPath.protocol")
    val specificationPath =
      config.getString(s"$OpenApiLabelsPath.specification-path")
    val useProxy = config.getString(s"$OpenApiLabelsPath.use-proxy")
    val openApiLabels =
      OpenApiLabels(port, protocol, specificationPath, useProxy).some

    val grpcPort   = config.getString(s"$GrpcLabelsPath.grpc-port")
    val grpcTls    = config.getString(s"$GrpcLabelsPath.grpc-tls")
    val grpcLabels = GrpcLabels(grpcPort, grpcTls).some

    Settings(dockerSettings,
             kubernetesSettings,
             Labels(serviceName, openApiLabels, grpcLabels))
  }
}
