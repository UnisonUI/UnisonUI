package restui.providers.git.git

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{FileSystemException, Files, Path}

import scala.jdk.CollectionConverters._
import scala.util._

import akka.stream.scaladsl.{Flow => AkkaFlow, Source => AkkaSource}
import cats.syntax.either._
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import io.circe.yaml.parser
import restui.Concurrency
import restui.models.{ContentType, Metadata, OpenApiFile, Service}
import restui.providers.git._
import restui.providers.git.git.data.{Repository, RestUI}
import restui.providers.git.process.{Process, ProcessArgs}
import restui.providers.git.settings.{Location, RepositorySettings}

object Git extends LazyLogging {
  val flow: Flow[Repository, Service] =
    AkkaFlow[Repository]
      .flatMapMerge(Concurrency.AvailableCore, cloneRepository(_).async)
      .flatMapMerge(Concurrency.AvailableCore, findFreshSwaggerFiles(_).async)
      .flatMapMerge(Concurrency.AvailableCore, retrieveSwaggerFilesAndGetLatestRef(_).async)
      .async
  private val GitCmd        = "git"
  private val DefaultBranch = "master"

  def source(repositories: Seq[RepositorySettings]): Source[Repository] =
    AkkaSource(repositories.collect {
      case RepositorySettings(Location.Uri(uri), branch, swaggerPaths) =>
        Repository(uri, branch.getOrElse(DefaultBranch), swaggerPaths)
    })

  private def cloneRepository(repo: Repository): Source[Repository] = {
    val repoWithDirectory = repo.copy(directory = Some(Files.createTempDirectory("restui-git-clone").toFile))
    execute(
      "clone" :: "--branch" :: repo.branch :: "--single-branch" :: repo.uri :: repoWithDirectory.directory.get.getAbsolutePath :: Nil).flatMapConcat {
      case Right(_) => AkkaSource.single(repoWithDirectory)
      case Left(exception) =>
        logger.warn("Error during cloning", exception)
        AkkaSource.empty[Repository]
    }
  }

  private def execute(args: List[String], cwd: Option[File] = None): Source[Either[String, List[String]]] =
    AkkaSource.single(ProcessArgs(GitCmd :: args, cwd)).via(Process.execute)

  private def findFreshSwaggerFiles(repo: Repository): Source[(Repository, List[Path])] = {
    val repoWithNewPath = findRestUIConfig(repo.directory.get.toPath).fold(repo) {
      case RestUI(serviceName, swaggerPaths) => repo.copy(swaggerPaths = swaggerPaths, serviceName = serviceName)
    }
    val localFiles = Files
      .walk(repo.directory.get.toPath)
      .iterator
      .asScala
      .to(LazyList)
      .filter(Files.isRegularFile(_))
      .map(_.normalize)

    AkkaSource.single(repoWithNewPath -> filterSwaggerFiles(repoWithNewPath, localFiles))
  }

  private def findRestUIConfig(path: Path): Option[RestUI] =
    Try {
      val yaml = new String(Files.readAllBytes(path.resolve(".restui.yaml")), StandardCharsets.UTF_8)
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

  private def filterSwaggerFiles(repo: Repository, files: LazyList[Path]): List[Path] = {
    val repoPath     = repo.directory.get.toPath
    val swaggerPaths = repo.swaggerPaths.map(repoPath.resolve(_).normalize)
    files.filter { file =>
      swaggerPaths.exists { swaggerPath =>
        file.startsWith(swaggerPath)
      }
    }.toList
  }

  private def retrieveSwaggerFilesAndGetLatestRef(repoWithFiles: (Repository, List[Path])): Source[Service] = {
    val (repo, files) = repoWithFiles

    AkkaSource(files)
      .flatMapMerge(Concurrency.AvailableCore, loadFile(_).async)
      .map {
        case (path, file) =>
          repo.directory.get.delete()
          val uri         = akka.http.scaladsl.model.Uri(repo.uri)
          val nameFromUri = uri.path.toString.substring(1)
          val serviceName = repo.serviceName.getOrElse(nameFromUri)
          val filePath    = repo.directory.get.toPath.relativize(path).toString
          val id          = s"$nameFromUri:$filePath"
          val provider    = uri.authority.host.address.split('.').head
          val metadata =
            Map(
              Metadata.Provider -> provider,
              "file"            -> filePath
            )
          Service(id, serviceName, file, metadata)
      }
      .async

  }

  private def loadFile(path: Path): Source[(Path, OpenApiFile)] =
    Try(new String(Files.readAllBytes(path), StandardCharsets.UTF_8)) match {
      case Success(content) =>
        val contentType = ContentType.fromString(path.toString)

        val openApiFile = OpenApiFile(contentType, content)
        AkkaSource.single(path -> openApiFile)
      case Failure(exception) =>
        logger.warn(s"Error while reading $path", exception)
        AkkaSource.empty[(Path, OpenApiFile)]
    }
}
