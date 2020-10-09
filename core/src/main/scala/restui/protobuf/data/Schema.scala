package restui.protobuf.data

import java.nio.file.{Files, Path}
import java.{util => ju}

import scala.collection.immutable.HashSet
import scala.jdk.CollectionConverters._
import scala.util.chaining._
import scala.util.control.Exception.allCatch

import cats.syntax.option._
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label._
import com.google.protobuf.DescriptorProtos.{FileDescriptorProto, FileDescriptorSet}
import com.google.protobuf.Descriptors._
import com.google.protobuf.{ByteString, MessageOrBuilder}
import io.circe.syntax._
import io.circe.{Encoder, Json}
import restui.protobuf.ProtobufCompiler

final case class Schema(messages: Map[String, MessageSchema] = Map.empty,
                        enums: Map[String, EnumSchema] = Map.empty,
                        services: Map[String, Service] = Map.empty,
                        rootKey: Option[String] = None) { val root: Option[MessageSchema] = rootKey.flatMap(messages.get(_)) }
object Schema {
  implicit class SchemaOps(path: Path)(implicit val protobufCompiler: ProtobufCompiler) {
    def toSchema: Either[Throwable, Schema] = Schema.fromFile(path)
  }

  implicit val encoder: Encoder[Schema] = (schema: Schema) =>
    Json.obj(
      "messages" -> schema.messages.values.asJson,
      "enums"    -> schema.enums.values.asJson,
      "services" -> schema.services.values.asJson
    )

  def fromFile(file: Path)(implicit protobufCompiler: ProtobufCompiler): Either[Throwable, Schema] =
    for {
      tempFile <- protobufCompiler.compile(file)
      result <-
        tempFile.toPath
          .pipe(Files.readAllBytes)
          .pipe(fromBytes)
      _ <- protobufCompiler.clean(tempFile)
    } yield result

  private def fromBytes(input: Array[Byte]): Either[Throwable, Schema] =
    allCatch.either {
      val descriptor      = FileDescriptorSet.parseFrom(input)
      val fileDescriptors = extraxtFileDescriptors(descriptor.getFileList().asScala.toList)
      val schemas = fileDescriptors.foldLeft(Schema()) {
        case (Schema(messageSchemas, enumSchemas, services, _), fileDescriptor) =>
          val (messages, enums) = parseDescriptors(fileDescriptor.getMessageTypes().asScala.toList).partitionMap {
            case (name, schema: MessageSchema) => Left(name -> schema)
            case (name, schema: EnumSchema)    => Right(name -> schema)
          }
          val allEnums = enums ++ parseEnumDescriptors(fileDescriptor.getEnumTypes().asScala.toList)
          Schema(messageSchemas ++ messages.toMap, enumSchemas ++ allEnums.toMap, services)
      }
      fileDescriptors.foldLeft(schemas) {
        case (schema @ Schema(_, _, services, _), fileDescriptor) =>
          val parsedServices = fileDescriptor.getServices().asScala.toList.pipe(parseServices(schemas))
          schema.copy(services = services ++ parsedServices)
      }
    }

  private def extraxtFileDescriptors(fileDescriptors: List[FileDescriptorProto]) = {
    val protoDescriptors = HashSet.from(fileDescriptors.map(_.getName))
    fileDescriptors
      .foldLeft(Map.empty[String, FileDescriptor]) { (resolved, protoDescriptor) =>
        if (resolved.contains(protoDescriptor.getName)) resolved
        else {
          val dependencies = protoDescriptor.getDependencyList.asScala.toList
          val resolvedList = dependencies.filter(protoDescriptors.contains).collect {
            Function.unlift { dependency =>
              resolved.get(dependency)
            }
          }
          if (resolvedList.size == dependencies.size) {
            val fileDescriptor = FileDescriptor.buildFrom(protoDescriptor, resolvedList.toArray)
            resolved + (protoDescriptor.getName -> fileDescriptor)
          } else resolved
        }
      }
      .values
      .toList
  }
  private def parseDescriptors(descriptors: List[Descriptor]): Map[String, DescriptorSchema] =
    descriptors.flatMap(descriptor => toSchemaMap(descriptor, Set(descriptor.getFullName))).toMap

  private def parseEnumDescriptors(descriptors: List[EnumDescriptor]): Map[String, EnumSchema] =
    descriptors.map(descriptor => descriptor.getFullName -> toEnumSchema(descriptor)).toMap

  private def toSchemaMap(descriptor: Descriptor, seen: Set[String]): Map[String, DescriptorSchema] = {
    val (fields, schemas) = descriptor.getFields.asScala.foldLeft(Map.empty[Int, Field], Map.empty[String, DescriptorSchema]) {
      case ((fields, schemas), fd) =>
        val default = if (fd.hasDefaultValue) {
          val value = fd.getDefaultValue
          val json = fd.getType match {
            case FieldDescriptor.Type.ENUM =>
              value.asInstanceOf[EnumValueDescriptor].getName
            case FieldDescriptor.Type.BYTES =>
              ju.Base64.getEncoder.encode(value.asInstanceOf[ByteString].toByteArray)
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
        fd.getType match {
          case FieldDescriptor.Type.MESSAGE =>
            val name   = fd.getMessageType.getFullName
            val schema = if (!seen(name)) toSchemaMap(fd.getMessageType, seen + name) else Map.empty
            (fields + (field.id -> field.copy(schema = Some(name))), schemas ++ schema)
          case FieldDescriptor.Type.ENUM =>
            val name   = fd.getEnumType.getFullName
            val schema = toEnumSchema(fd.getEnumType)
            (fields + (field.id -> field.copy(schema = Some(name))), schemas + (schema.name -> schema))
          case _ =>
            (fields + (field.id -> field), schemas)
        }
    }
    val msgOpts = optionMap(descriptor.getOptions)
    schemas + (descriptor.getFullName -> MessageSchema(descriptor.getFullName, fields, msgOpts))
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

  private def optionMap(options: MessageOrBuilder): Option[Map[String, String]] = {
    val optionsMap = options.getAllFields.asScala.foldLeft(Map.empty[String, String]) {
      case (map, (desc, ref)) => map + (desc.getFullName -> ref.toString)
    }
    if (optionsMap.nonEmpty) Some(optionsMap) else None
  }

  private def parseServices(schema: Schema)(services: List[ServiceDescriptor]): Map[String, Service] =
    services.map { service =>
      val methods = parseMethods(service.getMethods().asScala.toList, schema)
      service.getFullName() -> Service(service.getName(), service.getFullName(), methods)
    }.toMap

  private def parseMethods(methods: List[MethodDescriptor], schema: Schema): List[Method] =
    methods.map { method =>
      Method(
        method.getName(),
        schema.copy(rootKey = method.getInputType().getFullName().some),
        schema.copy(rootKey = method.getOutputType.getFullName().some)
      )
    }
}
