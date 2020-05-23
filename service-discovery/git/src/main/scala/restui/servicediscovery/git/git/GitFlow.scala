package restui.servicediscovery.git.git

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import scala.jdk.CollectionConverters._
import scala.util._

import akka.stream.scaladsl.{Flow, Source}
import org.slf4j.LoggerFactory
import restui.Concurrency
import restui.servicediscovery.git._
import restui.servicediscovery.git.process.{ProcessArgs, ProcessFlow}
import restui.servicediscovery.models.{ContentType, OpenApiFile}

object GitFlow {
  private val logger = LoggerFactory.getLogger(GitFlow.getClass)
  private val GitCmd = "git"

  val flow: F[Repo, (Repo, Seq[OpenApiFile])] =
    Flow[Repo]
      .flatMapMerge(Concurrency.AvailableCore, cloneRepo(_).async)
      .flatMapMerge(Concurrency.AvailableCore, findFreshSwaggerFiles(_).async)
      .flatMapMerge(Concurrency.AvailableCore, retrieveSwaggerFilesAndGetLatestRef(_).async)
      .async

  private def cloneRepo(repo: Repo): S[Repo] =
    exec("clone" :: "--branch" :: repo.branch :: "--single-branch" :: repo.uri :: repo.directory.getAbsolutePath :: Nil).flatMapConcat {
      case Right(_) => Source.single(repo)
      case Left(exception) =>
        logger.warn("Error during cloning: {}", exception)
        Source.empty[Repo]
    }

  private def findFreshSwaggerFiles(repo: Repo): S[(Repo, List[Path])] =
    repo.cachedRef match {
      case None =>
        val localFiles = Files
          .walk(repo.directory.toPath)
          .iterator
          .asScala
          .to(LazyList)
          .filter(Files.isRegularFile(_))
          .map(_.normalize)

        Source.single(repo -> filterSwaggerFiles(repo, localFiles))

      case Some(ref) =>
        val repoPath = repo.directory.toPath
        exec("diff" :: "--name-only" :: ref :: "HEAD" :: Nil, Some(repo.directory)).flatMapConcat {
          case Right(files) =>
            val normalisedFiles = files.to(LazyList).map(repoPath.resolve(_).normalize)
            Source.single(repo -> filterSwaggerFiles(repo, normalisedFiles))
          case Left(exception) =>
            logger.warn("Error during cloning: {}", exception)
            Source.empty[(Repo, List[Path])]
        }
    }

  private def filterSwaggerFiles(repo: Repo, files: LazyList[Path]): List[Path] = {
    val repoPath     = repo.directory.toPath
    val swaggerPaths = repo.swaggerPaths.map(repoPath.resolve(_).normalize)
    files.filter { file =>
      swaggerPaths.exists { swaggerPath =>
        file.startsWith(swaggerPath)
      }
    }.toList
  }
  private def retrieveSwaggerFilesAndGetLatestRef(repoWithFiles: (Repo, List[Path])): S[(Repo, Seq[OpenApiFile])] = {
    val (repo, files) = repoWithFiles
    val foundFilesSource = Source(files)
      .flatMapMerge(Concurrency.AvailableCore, loadFile(_).async)
      .fold(Seq.empty[OpenApiFile])(_ :+ _)
      .async
    latestRef(repo).zip(foundFilesSource)

  }
  private def latestRef(repo: Repo): S[Repo] =
    exec("rev-parse" :: "--verify" :: repo.branch :: Nil, Some(repo.directory)).flatMapConcat {
      case Right(result) => Source.single(repo.copy(cachedRef = Some(result.mkString.trim)))
      case Left(exception) =>
        logger.warn("Error while getting latest ref {}", exception)
        Source.empty[Repo]
    }

  private def loadFile(path: Path): S[OpenApiFile] =
    Try(new String(Files.readAllBytes(path), StandardCharsets.UTF_8)) match {
      case Success(content) =>
        val contentType = ContentType.fromString(path.toString)

        val openApiFile = OpenApiFile(contentType, content)
        Source.single(openApiFile)
      case Failure(exception) =>
        logger.warn(s"Error while reading $path", exception)
        Source.empty[OpenApiFile]
    }

  private def exec(args: List[String], cwd: Option[File] = None): S[Either[String, List[String]]] =
    Source.single(ProcessArgs(GitCmd :: args, cwd)).via(ProcessFlow.flow)
}
