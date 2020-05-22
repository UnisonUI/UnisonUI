package restui.servicediscovery.git.git

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import scala.jdk.CollectionConverters._
import scala.util._

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import org.slf4j.LoggerFactory
import restui.servicediscovery.git.process.{ProcessArgs, ProcessFlow}
import restui.servicediscovery.models.{ContentTypes, OpenApiFile}

object GitFlow {
  private val logger      = LoggerFactory.getLogger(GitFlow.getClass)
  private val GitCmd      = "git"
  private val Parallelism = 2
  def flow: Flow[Repo, (Repo, OpenApiFile), NotUsed] =
    Flow[Repo]
      .flatMapMerge(Parallelism, cloneRepo(_).async)
      .flatMapMerge(Parallelism, findModifiedFiles(_).async)
      .flatMapMerge(Parallelism, { case (repo, files) => latestRef(repo).map(newRepo => newRepo -> files).async })
      .flatMapMerge(Parallelism, { case (repo, files) => filterSwaggerFiles(repo, files).async })
      .flatMapMerge(Parallelism, { case (repo, path) => loadFile(repo, path).async })
      .async

  def cloneRepo(repo: Repo): Source[Repo, NotUsed] =
    exec("clone" :: "--branch" :: repo.branch :: "--single-branch" :: repo.uri :: repo.directory.getAbsolutePath :: Nil).flatMapConcat {
      case Success(_) => Source.single(repo)
      case Failure(exception) =>
        logger.warn("Error during cloning", exception)
        Source.empty[Repo]
    }

  def latestRef(repo: Repo): Source[Repo, NotUsed] =
    exec("rev-parse" :: "--verify" :: repo.branch :: Nil, Some(repo.directory)).flatMapConcat {
      case Success(result) => Source.single(repo.copy(cachedRef = Some(result.mkString.trim)))
      case Failure(exception) =>
        logger.warn("Error while getting latest ref", exception)
        Source.empty[Repo]
    }

  def findModifiedFiles(repo: Repo): Source[(Repo, List[Path]), NotUsed] =
    repo.cachedRef match {
      case None =>
        Source.single(
          repo -> Files
            .walk(repo.directory.toPath)
            .iterator
            .asScala
            .filter(Files.isRegularFile(_))
            .map(_.normalize)
            .toList)

      case Some(ref) =>
        val repoPath = repo.directory.toPath
        exec("diff" :: "--name-only" :: ref :: "HEAD" :: Nil, Some(repo.directory)).flatMapConcat {
          case Success(files) =>
            Source.single(repo -> files.map(repoPath.resolve(_).normalize))
          case Failure(exception) =>
            logger.warn("Error during cloning", exception)
            Source.empty[(Repo, List[Path])]
        }
    }

  def filterSwaggerFiles(repo: Repo, files: List[Path]): Source[(Repo, Path), NotUsed] = {
    val repoPath     = repo.directory.toPath
    val swaggerPaths = repo.swaggerPaths.map(repoPath.resolve(_).normalize)
    val filteredFiles = files.filter { file =>
      swaggerPaths.exists { swaggerPath =>
        file.startsWith(swaggerPath)
      }
    }.map { path =>
      repo -> path
    }
    Source(filteredFiles)
  }

  def loadFile(repo: Repo, path: Path): Source[(Repo, OpenApiFile), NotUsed] =
    Try(new String(Files.readAllBytes(path), StandardCharsets.UTF_8)) match {
      case Success(content) =>
        val contentType =
          if (path.endsWith("yaml") || path.endsWith("yml")) ContentTypes.Yaml
          else if (path.endsWith("json")) ContentTypes.Json
          else ContentTypes.Plain
        val openApiFile = OpenApiFile(contentType, content)
        Source.single(repo -> openApiFile)
      case Failure(exception) =>
        logger.warn("Error during cloning", exception)
        Source.empty[(Repo, OpenApiFile)]
    }

  def exec(args: List[String], cwd: Option[File] = None): Source[Try[List[String]], NotUsed] =
    Source.single(ProcessArgs(GitCmd :: args, cwd)).via(ProcessFlow.flow)
}
