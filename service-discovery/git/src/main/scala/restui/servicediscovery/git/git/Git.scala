package restui.servicediscovery.git.git

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
import restui.servicediscovery.git._
import restui.servicediscovery.git.git.data.{Repository, RestUI}
import restui.servicediscovery.git.process.{Process, ProcessArgs}
import restui.servicediscovery.git.settings.{Repository => RepositorySetting, Uri}
import restui.servicediscovery.models.{ContentType, OpenApiFile}

object Git extends LazyLogging {
  private val GitCmd = "git"
  def source(repositories: Seq[RepositorySetting]): Source[Repository] =
    AkkaSource(repositories.collect {
      case RepositorySetting(Uri(uri), swaggerPaths) =>
        Repository(uri, "master", swaggerPaths)
    })
  val flow: Flow[Repository, (Repository, OpenApiFile)] =
    AkkaFlow[Repository]
      .flatMapMerge(Concurrency.AvailableCore, cloneRepository(_).async)
      .flatMapMerge(Concurrency.AvailableCore, findFreshSwaggerFiles(_).async)
      .flatMapMerge(Concurrency.AvailableCore, retrieveSwaggerFilesAndGetLatestRef(_).async)
      .async

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

  private def findFreshSwaggerFiles(repo: Repository): Source[(Repository, List[Path])] = {
    val repoWithNewPath = findRestUIConfig(repo.directory.get.toPath).fold(repo) {
      case RestUI(swaggerPaths) => repo.copy(swaggerPaths = swaggerPaths)
    }
    repoWithNewPath.cachedRef match {
      case None =>
        val localFiles = Files
          .walk(repo.directory.get.toPath)
          .iterator
          .asScala
          .to(LazyList)
          .filter(Files.isRegularFile(_))
          .map(_.normalize)

        AkkaSource.single(repoWithNewPath -> filterSwaggerFiles(repoWithNewPath, localFiles))

      case Some(ref) =>
        val repoPath = repoWithNewPath.directory.get.toPath
        execute("diff" :: "--name-only" :: ref :: "HEAD" :: Nil, repo.directory).flatMapConcat {
          case Right(files) =>
            val normalisedFiles = files.to(LazyList).map(repoPath.resolve(_).normalize)
            AkkaSource.single(repoWithNewPath -> filterSwaggerFiles(repoWithNewPath, normalisedFiles))
          case Left(exception) =>
            logger.warn("Error during diff", exception)
            AkkaSource.empty[(Repository, List[Path])]
        }
    }
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
  private def retrieveSwaggerFilesAndGetLatestRef(repoWithFiles: (Repository, List[Path])): Source[(Repository, OpenApiFile)] = {
    val (repo, files) = repoWithFiles
    latestRef(repo).flatMapMerge(
      Concurrency.AvailableCore,
      repo =>
        AkkaSource(files)
          .flatMapMerge(Concurrency.AvailableCore, loadFile(_).async)
          .map { file =>
            repo.directory.get.delete()
            repo -> file
          }
          .async
    )

  }
  private def latestRef(repo: Repository): Source[Repository] =
    execute("rev-parse" :: "--verify" :: repo.branch :: Nil, repo.directory).flatMapConcat {
      case Right(result) => AkkaSource.single(repo.copy(cachedRef = Some(result.mkString.trim)))
      case Left(exception) =>
        logger.warn("Error while getting latest ref", exception)
        AkkaSource.empty[Repository]
    }

  private def loadFile(path: Path): Source[OpenApiFile] =
    Try(new String(Files.readAllBytes(path), StandardCharsets.UTF_8)) match {
      case Success(content) =>
        val contentType = ContentType.fromString(path.toString)

        val openApiFile = OpenApiFile(contentType, content)
        AkkaSource.single(openApiFile)
      case Failure(exception) =>
        logger.warn(s"Error while reading $path", exception)
        AkkaSource.empty[OpenApiFile]
    }

  private def execute(args: List[String], cwd: Option[File] = None): Source[Either[String, List[String]]] =
    AkkaSource.single(ProcessArgs(GitCmd :: args, cwd)).via(Process.execute)
}
