package restui.providers.git.git

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{FileSystemException, Files, Path, Paths}

import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._
import scala.util._

import akka.stream.SourceShape
import akka.stream.scaladsl.{Broadcast, Flow => AkkaFlow, GraphDSL, Merge, Source => AkkaSource}
import cats.syntax.either._
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import io.circe.yaml.parser
import restui.Concurrency
import restui.models.{Metadata, Service}
import restui.providers.git._
import restui.providers.git.git.data.{Repository, RestUI}
import restui.providers.git.process.{Process, ProcessArgs}
import restui.providers.git.settings.{Location, RepositorySettings}

object Git extends LazyLogging {
  private val GitCmd        = "git"
  private val DefaultBranch = "master"
  private type RepositoryWithSha = (Repository, Option[String])
  private type Files             = (Repository, List[Path])
  private type FilesWithSha      = (Files, Option[String])

  private val outboundFlow: Flow[FilesWithSha, Service] =
    AkkaFlow[FilesWithSha].flatMapMerge(Concurrency.AvailableCore, { case (files, _) => retrieveSpecificationFiles(files) })

  private val cloneOrFetch: Flow[RepositoryWithSha, FilesWithSha] =
    AkkaFlow[RepositoryWithSha].flatMapMerge(
      Concurrency.AvailableCore,
      {
        case (repository, hash) =>
          val source = hash match {
            case None =>
              cloneRepository(repository).map { repository =>
                val localFiles = Files
                  .walk(repository.directory.get.toPath)
                  .iterator
                  .asScala
                  .to(LazyList)
                  .filter(Files.isRegularFile(_))
                  .map(_.normalize)
                  .toList
                repository -> localFiles
              }
            case Some(sha1) => pullRepository(repository).flatMapConcat(changedFiles(_, sha1))
          }
          source.map(_ -> hash)
      }
    )

  private val findSpecificationFiles: Flow[FilesWithSha, FilesWithSha] = AkkaFlow[FilesWithSha].map {
    case ((repository, files), sha1) =>
      val repositoryWithNewPath = findRestUIConfig(repository.directory.get.toPath).fold(repository) {
        case RestUI(serviceName, specificationPaths) => repository.copy(specificationPaths = specificationPaths, serviceName = serviceName)
      }
      (repositoryWithNewPath -> filterSpecificationsFiles(repositoryWithNewPath, files) -> sha1)
  }

  private val latestSha1: Flow[FilesWithSha, FilesWithSha] = AkkaFlow[FilesWithSha].flatMapMerge(
    Concurrency.AvailableCore,
    {
      case ((repository, files), _) =>
        execute("rev-parse" :: "--verify" :: repository.branch :: Nil, repository.directory).flatMapConcat {
          case Right(sha1 :: _) => AkkaSource.single(repository -> files, Some(sha1))
          case Left(exception) =>
            logger.warn("Error during latest sha1", exception)
            AkkaSource.empty[FilesWithSha]
        }
    }
  )

  def fromSettings(cacheDuration: FiniteDuration, repositories: Seq[RepositorySettings]): Source[Service] =
    fromSource(
      cacheDuration,
      AkkaSource(repositories.collect {
        case RepositorySettings(Location.Uri(uri), branch, specificationPaths) =>
          Repository(uri, branch.getOrElse(DefaultBranch), specificationPaths)
      })
    )

  def fromSource(cacheDuration: FiniteDuration, repositories: Source[Repository]): Source[Service] =
    AkkaSource.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val delayFlow: Flow[FilesWithSha, RepositoryWithSha] =
        AkkaFlow[FilesWithSha].delay(cacheDuration).map { case ((repository, _), sha1) => repository -> sha1 }
      val init      = builder.add(repositories.map(_ -> None))
      val outbound  = builder.add(outboundFlow.async)
      val delay     = builder.add(delayFlow)
      val merge     = builder.add(Merge[RepositoryWithSha](2))
      val broadcast = builder.add(Broadcast[FilesWithSha](2, eagerCancel = true))

      // format: OFF
      init ~> merge ~> cloneOrFetch ~> findSpecificationFiles ~> latestSha1 ~> broadcast ~> outbound
              merge <~ delay <~ broadcast
      // format: ON
      SourceShape(outbound.out)
    })

  private def cloneRepository(repo: Repository): Source[Repository] = {
    val repoWithDirectory = repo.copy(directory = Some(Files.createTempDirectory("restui-git-clone").toFile))
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

  private def changedFiles(repository: Repository, sha1: String): Source[Files] =
    execute("diff" :: "--name-only" :: sha1 :: "HEAD" :: Nil, repository.directory).flatMapConcat {
      case Right(files) =>
        val repoPath = repository.directory.get.toPath
        AkkaSource.single(repository -> files.map(file => repoPath.resolve(Paths.get(file)).normalize))
      case Left(exception) =>
        logger.warn(s"Error during changed: $exception")
        AkkaSource.empty[Files]
    }

  private def execute(args: List[String], cwd: Option[File] = None): Source[Either[String, List[String]]] =
    AkkaSource.single(ProcessArgs(GitCmd :: args, cwd)).via(Process.execute)

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

  private def filterSpecificationsFiles(repo: Repository, files: List[Path]): List[Path] = {
    val repoPath           = repo.directory.get.toPath
    val specificationPaths = repo.specificationPaths.map(repoPath.resolve(_).normalize)
    files.filter { file =>
      specificationPaths.exists { specificationPath =>
        file.startsWith(specificationPath)
      }
    }
  }

  private def retrieveSpecificationFiles(repoWithFiles: Files): Source[Service] = {
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
              Metadata.File     -> filePath
            )
          Service(id, serviceName, file, metadata)
      }
      .async

  }

  private def loadFile(path: Path): Source[(Path, String)] =
    Try(new String(Files.readAllBytes(path), StandardCharsets.UTF_8)) match {
      case Success(content) =>
        AkkaSource.single(path -> content)
      case Failure(exception) =>
        logger.warn(s"Error while reading $path", exception)
        AkkaSource.empty[(Path, String)]
    }
}
