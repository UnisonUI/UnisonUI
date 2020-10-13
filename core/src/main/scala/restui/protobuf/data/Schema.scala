package restui.protobuf.data

import java.nio.file.{Files, Path}
import java.{util => ju}

import cats.syntax.option._
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label._
import com.google.protobuf.DescriptorProtos.{
  FileDescriptorProto,
  FileDescriptorSet
}
import com.google.protobuf.Descriptors._
import com.google.protobuf.{ByteString, MessageOrBuilder}
import io.circe.syntax._
import io.circe.{Encoder, Json}
import restui.protobuf.ProtobufCompiler

import scala.collection.immutable.HashSet
import scala.jdk.CollectionConverters._
import scala.util.chaining._
import scala.util.control.Exception.allCatch

final case class Schema(messages: Map[String, MessageSchema] = Map.empty,
                        enums: Map[String, EnumSchema] = Map.empty,
                        services: Map[String, Service] = Map.empty,
                        rootKey: Option[String] = None) {
  val root: Option[MessageSchema] = rootKey.flatMap(messages.get)
}

object Schema {
  implicit class SchemaOps(path: Path)(implicit
      val protobufCompiler: ProtobufCompiler) {
    def toSchema: Either[Throwable, Schema] = Schema.fromFile(path)
  }

  implicit val encoder: Encoder[Schema] = (schema: Schema) =>
    Json.obj(
      "messages" -> schema.messages.values.asJson,
      "enums"    -> schema.enums.values.asJson,
      "services" -> schema.services.values.asJson
    )

  def fromFile(file: Path)(implicit
      protobufCompiler: ProtobufCompiler): Either[Throwable, Schema] =
    for {
      tempFile <- protobufCompiler.compile(file)
      result <-
        tempFile.toPath
          .pipe(Files.readAllBytes)
          .pipe(fromBytes)
      _ <- protobufCompiler.clean(tempFile)
    } yield result

  def fromFileDescriptorProtos(
      input: Vector[Array[Byte]]): Either[Throwable, Schema] =
    for {
      descriptors <- allCatch.either(input.map(FileDescriptorProto.parseFrom))
      schema      <- descriptors.pipe(decode)
    } yield schema

  def fromBytes(input: Array[Byte]): Either[Throwable, Schema] = for {
    descriptor <- allCatch.either(FileDescriptorSet.parseFrom(input))
    schema <- descriptor.getFileList.asScala.toVector
      .pipe(decode)
  } yield schema

  private def decode(
      descriptors: Vector[FileDescriptorProto]): Either[Throwable, Schema] =
    allCatch.either {
      val fileDescriptors = extractFileDescriptors(descriptors)
      val schemas = fileDescriptors.foldLeft(Schema()) {
        case (Schema(messageSchemas, enumSchemas, services, _),
              fileDescriptor) =>
          val (messages, enums) = parseDescriptors(
            fileDescriptor.getMessageTypes.asScala.toVector).partitionMap {
            case (name, schema: MessageSchema) => Left(name -> schema)
            case (name, schema: EnumSchema)    => Right(name -> schema)
          }
          val allEnums = enums ++ parseEnumDescriptors(
            fileDescriptor.getEnumTypes.asScala.toVector)
          Schema(messageSchemas ++ messages.toMap,
                 enumSchemas ++ allEnums.toMap,
                 services)
      }
      fileDescriptors.foldLeft(schemas) {
        case (schema @ Schema(_, _, services, _), fileDescriptor) =>
          val parsedServices = fileDescriptor.getServices.asScala.toVector
            .pipe(parseServices(schemas))
          schema.copy(services = services ++ parsedServices)
      }
    }

  private def extractFileDescriptors(
      fileDescriptors: Vector[FileDescriptorProto]) = {
    val protoDescriptors = HashSet.from(fileDescriptors.map(_.getName))
    fileDescriptors
      .foldLeft(Map.empty[String, FileDescriptor]) {
        (resolved, protoDescriptor) =>
          if (resolved.contains(protoDescriptor.getName)) resolved
          else {
            val dependencies =
              protoDescriptor.getDependencyList.asScala.toVector
            val resolvedVector =
              dependencies.filter(protoDescriptors.contains).collect {
                Function.unlift { dependency =>
                  resolved.get(dependency)
                }
              }
            if (resolvedVector.size == dependencies.size) {
              val fileDescriptor =
                FileDescriptor.buildFrom(protoDescriptor,
                                         resolvedVector.toArray)
              resolved + (protoDescriptor.getName -> fileDescriptor)
            } else resolved
          }
      }
      .values
      .toVector
  }
  private def parseDescriptors(
      descriptors: Vector[Descriptor]): Map[String, DescriptorSchema] =
    descriptors
      .flatMap(descriptor =>
        toSchemaMap(descriptor, Set(descriptor.getFullName)))
      .toMap

  private def parseEnumDescriptors(
      descriptors: Vector[EnumDescriptor]): Map[String, EnumSchema] =
    descriptors
      .map(descriptor => descriptor.getFullName -> toEnumSchema(descriptor))
      .toMap

  private def toSchemaMap(descriptor: Descriptor,
                          seen: Set[String]): Map[String, DescriptorSchema] = {
    val oneOfMap = (for {
      oneOfDescriptor <- descriptor.getOneofs.asScala
      field           <- oneOfDescriptor.getFields.asScala
    } yield field.getNumber -> oneOfDescriptor.getName).toMap

    val (fields, oneOfs, schemas) = descriptor.getFields.asScala
      .foldLeft(Map.empty[Int, Field],
                Map.empty[String, Map[Int, Field]],
                Map.empty[String, DescriptorSchema]) {
        case ((fields, oneOfs, schemas), fd) =>
          val default = if (fd.hasDefaultValue) {
            val value = fd.getDefaultValue
            val json = fd.getType match {
              case FieldDescriptor.Type.ENUM =>
                value.asInstanceOf[EnumValueDescriptor].getName
              case FieldDescriptor.Type.BYTES =>
                ju.Base64.getEncoder.encode(
                  value.asInstanceOf[ByteString].toByteArray)
              case _ => value
            }
            Some(json)
          } else
            None
          val fieldOpts = optionMap(fd.getOptions)
          val field =
            Field(
              fd.getNumber,
              fd.getName,
              getLabel(fd),
              fd.getType,
              fd.isPacked,
              default,
              None,
              fieldOpts
            )
          val (newField, newSchemas) = fd.getType match {
            case FieldDescriptor.Type.MESSAGE =>
              val name = fd.getMessageType.getFullName
              val schema =
                if (!seen(name)) toSchemaMap(fd.getMessageType, seen + name)
                else Map.empty
              (field.copy(schema = Some(name)), schemas ++ schema)
            case FieldDescriptor.Type.ENUM =>
              val name   = fd.getEnumType.getFullName
              val schema = toEnumSchema(fd.getEnumType)
              (field.copy(schema = Some(name)),
               schemas + (schema.name -> schema))
            case _ =>
              (field, schemas)
          }
          oneOfMap
            .get(newField.id)
            .fold((fields + (newField.id -> newField), oneOfs, newSchemas)) {
              oneOfName =>
                val newOneOfs =
                  oneOfs.getOrElse(oneOfName,
                                   Map.empty) + (newField.id -> newField)
                (fields, oneOfs + (oneOfName                 -> newOneOfs), newSchemas)
            }
      }
    val msgOpts = optionMap(descriptor.getOptions)

    schemas + (descriptor.getFullName -> MessageSchema(descriptor.getFullName,
                                                       fields,
                                                       msgOpts,
                                                       oneOfs))
  }

  private def toEnumSchema(ed: EnumDescriptor): EnumSchema = {
    val values = ed.getValues.asScala.map(v => v.getNumber -> v.getName).toMap
    EnumSchema(ed.getFullName, values, optionMap(ed.getOptions))
  }

  private def getLabel(fd: FieldDescriptor): Label =
    fd.toProto.getLabel match {
      case LABEL_REQUIRED => Label.Required
      case LABEL_OPTIONAL => Label.Optional
      case LABEL_REPEATED => Label.Repeated
    }

  private def optionMap(
      options: MessageOrBuilder): Option[Map[String, String]] = {
    val optionsMap =
      options.getAllFields.asScala.foldLeft(Map.empty[String, String]) {
        case (map, (desc, ref)) => map + (desc.getFullName -> ref.toString)
      }
    if (optionsMap.nonEmpty) Some(optionsMap) else None
  }

  private def parseServices(schema: Schema)(
      services: Vector[ServiceDescriptor]): Map[String, Service] =
    services.map { service =>
      val methods = parseMethods(service.getMethods.asScala.toVector, schema)
      service.getFullName -> Service(service.getName,
                                     service.getFullName,
                                     methods)
    }.toMap

  private def parseMethods(methods: Vector[MethodDescriptor],
                           schema: Schema): Vector[Method] =
    methods.map { method =>
      Method(
        method.getName,
        schema.copy(rootKey = method.getInputType.getFullName.some),
        schema.copy(rootKey = method.getOutputType.getFullName.some),
        method.isServerStreaming,
        method.isClientStreaming
      )
    }
}
