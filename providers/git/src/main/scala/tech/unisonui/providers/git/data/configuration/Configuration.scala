package tech.unisonui.providers.git.data.configuration

import io.circe.generic.auto._
import io.circe.{Decoder, HCursor}
import tech.unisonui.models.Service
import tech.unisonui.providers.git.data.OpenApi

final case class Configuration(name: Option[String],
                               openapi: Option[OpenApiSetting],
                               grpc: Option[GrpcSetting])

final case class OpenApiSetting(specifications: List[OpenApi],
                                useProxy: Boolean)

final case class GrpcSetting(servers: Map[String, Service.Grpc.Server],
                             protobufFiles: Map[String, ProtobufSetting])

final case class ProtobufSetting(maybeName: Option[String],
                                 servers: Map[String, Service.Grpc.Server])

object Configuration {
  implicit val decoder: Decoder[Configuration] = (cursor: HCursor) =>
    for {
      name           <- cursor.get[Option[String]]("name")
      openapi        <- cursor.get[Option[OpenApiSetting]]("openapi")
      specifications <- cursor.get[Option[List[OpenApi]]]("specifications")
      grpc           <- cursor.get[Option[GrpcSetting]]("grpc")
      useProxy       <- cursor.getOrElse[Boolean]("useProxy")(false)
      openapiSettings = openapi
        .orElse(specifications.map {
          OpenApiSetting(_, useProxy)
        })
    } yield Configuration(name, openapiSettings, grpc)
}

object OpenApiSetting {
  implicit val decoder: Decoder[OpenApiSetting] = (cursor: HCursor) =>
    for {
      specifications <- cursor.get[List[OpenApi]]("specifications")
      useProxy       <- cursor.getOrElse[Boolean]("useProxy")(false)
    } yield OpenApiSetting(specifications, useProxy)
}

object GrpcSetting {
  import ProtobufSetting.serverDecoder
  implicit val decoder: Decoder[GrpcSetting] = (cursor: HCursor) =>
    for {
      servers <- cursor
        .getOrElse[List[(String, Service.Grpc.Server)]]("servers")(Nil)
      protobufFiles <- cursor.getOrElse[Map[String, ProtobufSetting]](
        "protobufs")(Map.empty)
    } yield GrpcSetting(servers.toMap, protobufFiles)
}

object ProtobufSetting {
  implicit val decoder: Decoder[ProtobufSetting] = (cursor: HCursor) =>
    for {
      maybeName <- cursor.get[Option[String]]("name")
      servers <- cursor
        .getOrElse[List[(String, Service.Grpc.Server)]]("servers")(Nil)
    } yield ProtobufSetting(maybeName, servers.toMap)

  implicit val serverDecoder: Decoder[(String, Service.Grpc.Server)] =
    (cursor: HCursor) =>
      for {
        address <- cursor.get[String]("address")
        port    <- cursor.get[Int]("port")
        useTls  <- cursor.getOrElse[Boolean]("useTls")(false)
        name    <- cursor.getOrElse[String]("name")(s"$address:$port")
      } yield name -> Service.Grpc.Server(address, port, useTls)
}
