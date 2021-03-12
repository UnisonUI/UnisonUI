package tech.unisonui.providers.container.settings

import cats.syntax.option._

import scala.util.control.Exception.allCatch

final case class Labels(serviceName: String,
                        openapi: Option[OpenApiLabels],
                        grpc: Option[GrpcLabels]) {
  def extractLabels(labels: Map[String, String]): Option[Labels] = {
    val openapi = this.openapi.flatMap { openApiLabels =>
      for {
        port <- labels
          .get(openApiLabels.port)
          .filter(port => allCatch.opt(port.toInt).isDefined)
        specificationPath =
          labels.getOrElse(openApiLabels.specificationPath,
                           "/specification.yaml")
        protocol = labels.getOrElse(openApiLabels.protocol, "http")
        useProxy = labels.getOrElse(openApiLabels.useProxy, "false")
      } yield OpenApiLabels(port, protocol, specificationPath, useProxy)
    }
    val grpc = this.grpc.flatMap { grpcLabels =>
      for {
        port <- labels
          .get(grpcLabels.port)
          .filter(port => allCatch.opt(port.toInt).isDefined)
        tls = labels.getOrElse(grpcLabels.tls, "false")
      } yield GrpcLabels(port, tls)
    }
    copy(openapi = openapi, grpc = grpc).some
  }
}

final case class OpenApiLabels(
    port: String,
    protocol: String,
    specificationPath: String,
    useProxy: String
)

final case class GrpcLabels(
    port: String,
    tls: String
)
