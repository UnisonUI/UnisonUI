package restui.providers.git.git

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{FileSystemException, Files, Path, Paths}

import akka.stream.SourceShape
import akka.stream.scaladsl.{
  Broadcast,
  Flow => AkkaFlow,
  GraphDSL,
  Merge,
  Source => AkkaSource
}
import cats.syntax.either._
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import io.circe.yaml.parser
import restui.models.{Metadata, Service, ServiceEvent}
import restui.protobuf.ProtobufCompiler
import restui.protobuf.data.Schema._
import restui.providers.git._
import restui.providers.git.git.data._
import restui.providers.git.process.{Process, ProcessArgs}
import restui.providers.git.settings.{Location, RepositorySettings}

import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._
import scala.util._
import scala.util.chaining._

object Git extends LazyLogging {
  private val GitCmd        = "git"
  private val DefaultBranch = "master"
  private type RepositoryWithSha     = (Repository, Option[String])
  private type Files                 = (Repository, List[(Option[String], Path)])
  private type FileEvents            = (Repository, List[GitFileEvent])
  private type FilesWithSha          = (Files, Option[String])
  private type FilesWithShaWithEvent = (FileEvents, Option[String])

  private def outboundFlow(implicit protobufCompiler: ProtobufCompiler)
      : Flow[FilesWithShaWithEvent, ServiceEvent] =
    AkkaFlow[FilesWithShaWithEvent].flatMapConcat { case (files, _) =>
      retrieveSpecificationFiles(files)
    }

  private val cloneOrFetch: Flow[RepositoryWithSha, FilesWithSha] =
    AkkaFlow[RepositoryWithSha].flatMapConcat { case (repository, hash) =>
      val source = hash match {
        case None =>
          cloneRepository(repository).map { repository =>
            val localFiles = Files
              .walk(repository.directory.get.toPath)
              .iterator
              .asScala
              .to(LazyList)
              .filter(Files.isRegularFile(_))
              .map(path => None -> path.normalize)
              .toList
            repository -> localFiles

          }
        case Some(sha1) =>
          pullRepository(repository).flatMapConcat(changedFiles(_, sha1))
      }
      source.map(_ -> hash)
    }

  private def changedFiles(repository: Repository,
                           sha1: String): Source[Files] =
    execute("diff" :: "--name-only" :: sha1 :: "HEAD" :: Nil,
            repository.directory).flatMapConcat {
      case Right(files) =>
        val repoPath = repository.directory.get.toPath
        AkkaSource.single(repository -> files.map(file =>
          None -> repoPath.resolve(Paths.get(file)).normalize))
      case Left(exception) =>
        logger.warn(s"Error during changed: $exception")
        AkkaSource.empty[Files]
    }

  private val findSpecificationFiles
      : Flow[FilesWithSha, FilesWithShaWithEvent] = AkkaFlow[FilesWithSha].map {
    case ((repository, files), sha1) =>
      val (repositoryWithNewPath, rootUseProxy, grpcSettings) =
        findRestUIConfig(repository.directory.get.toPath)
          .fold(
            (repository,
             Option.empty[Boolean],
             Map.empty[String, ProtobufSetting])) {
            case RestUI(serviceName,
                        specificationPaths,
                        maybeGrpcSettings,
                        useProxy) =>
              val grpcSettings =
                maybeGrpcSettings.fold(Map.empty[String, ProtobufSetting]) {
                  settings =>
                    settings.protobufFiles.view.mapValues {
                      case setting @ ProtobufSetting(_, servers)
                          if servers.isEmpty =>
                        setting.copy(servers = settings.servers)
                      case setting => setting
                    }.toMap
                }
              (repository.copy(specificationPaths = specificationPaths,
                               serviceName = serviceName),
               useProxy,
               grpcSettings)
          }

      val repoPath = repository.directory.get.toPath
      val toAdd = filterSpecificationsFiles(repositoryWithNewPath,
                                            files,
                                            grpcSettings,
                                            rootUseProxy)
      val toDelete = repository.specificationPaths.filter { spec =>
        !repositoryWithNewPath.specificationPaths.exists(newSpec =>
          newSpec.path == spec.path)
      }.map { spec =>
        spec.path
          .pipe(repoPath.resolve)
          .normalize
          .pipe(GitFileEvent.Deleted)
      }
      val events = toDelete ++ toAdd
      (repositoryWithNewPath -> events -> sha1)
  }

  private val latestSha1: Flow[FilesWithShaWithEvent, FilesWithShaWithEvent] =
    AkkaFlow[FilesWithShaWithEvent].flatMapConcat {
      case ((repository, files), _) =>
        execute("rev-parse" :: "--verify" :: repository.branch :: Nil,
                repository.directory).flatMapConcat {
          case Right(sha1 :: _) =>
            AkkaSource.single(repository -> files, Some(sha1))
          case Right(Nil) =>
            logger.warn("Error during latest sha1")
            AkkaSource.empty[FilesWithShaWithEvent]
          case Left(exception) =>
            logger.warn("Error during latest sha1", exception)
            AkkaSource.empty[FilesWithShaWithEvent]
        }
    }

  def fromSettings(cacheDuration: FiniteDuration,
                   repositories: Seq[RepositorySettings])(implicit
      protobufCompiler: ProtobufCompiler): Source[ServiceEvent] =
    fromSource(
      cacheDuration,
      AkkaSource(repositories.collect {
        case RepositorySettings(Location.Uri(uri),
                                branch,
                                specificationPaths,
                                useProxy) =>
          Repository(uri,
                     branch.getOrElse(DefaultBranch),
                     specificationPaths.map(UnnamedSpecification(_)),
                     useProxy = useProxy)
      })
    )

  def fromSource(cacheDuration: FiniteDuration,
                 repositories: Source[Repository])(implicit
      protobufCompiler: ProtobufCompiler): Source[ServiceEvent] =
    AkkaSource.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val delayFlow: Flow[FilesWithShaWithEvent, RepositoryWithSha] =
        AkkaFlow[FilesWithShaWithEvent].delay(cacheDuration).map {
          case ((repository, _), sha1) => repository -> sha1
        }
      val init     = builder.add(repositories.map(_ -> None))
      val outbound = builder.add(outboundFlow.async)
      val delay    = builder.add(delayFlow)
      val merge    = builder.add(Merge[RepositoryWithSha](2))
      val broadcast =
        builder.add(Broadcast[FilesWithShaWithEvent](2, eagerCancel = true))

      // format: OFF
      init ~> merge ~> cloneOrFetch ~> findSpecificationFiles ~> latestSha1 ~> broadcast ~> outbound
              merge <~ delay <~ broadcast
      // format: ON
      SourceShape(outbound.out)
    })

  private def cloneRepository(repo: Repository): Source[Repository] = {
    val repoWithDirectory = repo.copy(directory =
      Some(Files.createTempDirectory("restui-git-clone").toFile))
    execute(
      "clone" :: "--branch" :: repo.branch :: "--single-branch" :: "--depth" :: "1" :: repo.uri :: repoWithDirectory.directory.get.getAbsolutePath :: Nil).flatMapConcat {
      case Right(_) => AkkaSource.single(repoWithDirectory)
      case Left(exception) =>
        logger.warn(s"Error during cloning: $exception")
        AkkaSource.empty[Repository]
    }
  }

  private def pullRepository(repository: Repository): Source[Repository] =
    execute("pull" :: Nil, repository.directory).flatMapConcat {
      case Right(_) => AkkaSource.single(repository)
      case Left(exception) =>
        logger.warn(s"Error during pulling: $exception")
        AkkaSource.empty[Repository]
    }

  private def execute(
      args: List[String],
      cwd: Option[File] = None): Source[Either[String, List[String]]] =
    AkkaSource.single(ProcessArgs(GitCmd :: args, cwd)).via(Process.execute)

  private def findRestUIConfig(path: Path): Option[RestUI] =
    Try {
      val yaml = new String(Files.readAllBytes(path.resolve(".restui.yaml")),
                            StandardCharsets.UTF_8)
      parser
        .parse(yaml)
        .flatMap(_.as[RestUI])
        .valueOr(throw _)
    } match {
      case Success(config)                 => Some(config)
      case Failure(_: FileSystemException) => None
      case Failure(exception) =>
        logger.warn("Error while reading restui config file", exception)
        None
    }

  private def filterSpecificationsFiles(
      repo: Repository,
      files: List[(Option[String], Path)],
      grpcSettings: Map[String, ProtobufSetting],
      rootUseProxy: Option[Boolean]): List[GitFileEvent] = {
    val repoPath = repo.directory.get.toPath
    val specificationPaths = repo.specificationPaths.map {
      case UnnamedSpecification(path) =>
        (None, repoPath.resolve(path).normalize, rootUseProxy)
      case NamedSpecification(name, path, useProxy) =>
        (Some(name),
         repoPath.resolve(path).normalize,
         useProxy.orElse(rootUseProxy))
    }
    files.collect {
      Function.unlift { case (_, file) =>
        specificationPaths.find { case (_, specificationPath, _) =>
          file.startsWith(specificationPath)
        }.map { case (name, _, useProxy) =>
          GitFileEvent.UpsertedOpenApi(name, file, useProxy)
        }.orElse {
          grpcSettings.find { case (path, _) =>
            path.pipe(repoPath.resolve).normalize.pipe(file.startsWith)
          }.map { case (_, ProtobufSetting(name, servers)) =>
            GitFileEvent.UpsertedGrpc(name, file, servers)
          }
        }
      }
    }
  }

  private def retrieveSpecificationFiles(repoWithFiles: FileEvents)(implicit
      protobufCompiler: ProtobufCompiler): Source[ServiceEvent] = {
    val (repo, files) = repoWithFiles

    val uri         = akka.http.scaladsl.model.Uri(repo.uri)
    val nameFromUri = uri.path.toString.substring(1)

    AkkaSource(files)
      .flatMapConcat(loadFile(_).async)
      .map {
        case LoadedContent.OpenApi(maybeName, path, content, maybeUseProxy) =>
          val serviceName =
            maybeName.getOrElse(repo.serviceName.getOrElse(nameFromUri))
          val filePath = repo.directory.get.toPath.relativize(path).toString
          val id       = s"$nameFromUri:$filePath"
          val provider = uri.authority.host.address.split('.').head
          val useProxy = maybeUseProxy.getOrElse(repo.useProxy)
          val metadata =
            Map(
              Metadata.Provider -> provider,
              Metadata.File     -> filePath
            )
          ServiceEvent.ServiceUp(
            Service.OpenApi(id, serviceName, content, metadata, useProxy))
        case LoadedContent.Grpc(maybeName, path, schema, servers) =>
          val serviceName =
            maybeName.getOrElse(repo.serviceName.getOrElse(nameFromUri))
          val filePath = repo.directory.get.toPath.relativize(path).toString
          val id       = s"$nameFromUri:$filePath"
          val provider = uri.authority.host.address.split('.').head
          val metadata =
            Map(
              Metadata.Provider -> provider,
              Metadata.File     -> filePath
            )
          ServiceEvent.ServiceUp(
            Service.Grpc(id, serviceName, schema, servers, metadata))
        case LoadedContent.Deleted(path) =>
          val filePath = repo.directory.get.toPath.relativize(path).toString
          val id       = s"$nameFromUri:$filePath"
          ServiceEvent.ServiceDown(id)
      }
      .async

  }

  private def loadFile(event: GitFileEvent)(implicit
      protobufCompiler: ProtobufCompiler): Source[LoadedContent] =
    event match {
      case GitFileEvent.Deleted(path) =>
        AkkaSource.single(LoadedContent.Deleted(path))
      case GitFileEvent.UpsertedOpenApi(maybeName, path, useProxy) =>
        Try(
          new String(Files.readAllBytes(path), StandardCharsets.UTF_8)) match {
          case Success(content) =>
            AkkaSource.single(
              LoadedContent.OpenApi(maybeName, path, content, useProxy))
          case Failure(exception) =>
            logger.warn(s"Error while reading $path", exception)
            AkkaSource.single(LoadedContent.Deleted(path))
        }
      case GitFileEvent.UpsertedGrpc(maybeName, path, servers) =>
        path.toSchema match {
          case Right(schema) =>
            AkkaSource.single(
              LoadedContent.Grpc(maybeName, path, schema, servers))
          case Left(exception) =>
            logger.warn(s"Error while reading $path", exception)
            AkkaSource.single(LoadedContent.Deleted(path))

        }
    }
}
