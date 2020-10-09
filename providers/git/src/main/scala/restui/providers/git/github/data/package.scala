package restui.providers.git.github

import cats.syntax.either._
import cats.syntax.functor._
import io.circe.{Decoder, Encoder, HCursor, Json}

package object data {
  sealed trait GrahpQL
  final case class Error(messages: List[String]) extends GrahpQL

  object Error {
    implicit val encoder: Encoder[Error] = (error: Error) =>
      Json.obj(
        "errors" -> Json.arr(error.messages.map { message =>
          Json.obj("message" -> Json.fromString(message))
        }: _*)
      )

    implicit val decoder: Decoder[Error] = (cursor: HCursor) =>
      cursor
        .get[String]("message")
        .map(List(_))
        .leftFlatMap(_ =>
          cursor
            .get[List[Json]]("errors")
            .map(_.flatMap(_.hcursor.get[String]("message").toOption)))
        .map(Error(_))
  }

  final case class Repository(repositories: Seq[Node], cursor: Option[String])
      extends GrahpQL
  final case class Node(name: String, url: String, branch: String)

  object Repository {

    implicit val encoder: Encoder[Repository] = (repository: Repository) =>
      Json.obj(
        "data" -> Json.obj(
          "viewer" -> Json.obj(
            "repositories" -> Json.obj(
              "pageInfo" -> Json.obj(
                "hasNextPage" -> Json.fromBoolean(repository.cursor.isDefined),
                "endCursor"   -> Json.fromString(repository.cursor.getOrElse(""))
              ),
              "nodes" -> Json.arr(
                repository.repositories.map(node =>
                  Json.obj(
                    "nameWithOwner" -> Json.fromString(node.name),
                    "url"           -> Json.fromString(node.url),
                    "defaultBranchRef" -> Json.obj(
                      "name" -> Json.fromString(node.branch)
                    )
                  )): _*)
            )
          )
        )
      )

    implicit val decoder: Decoder[Repository] = (cursor: HCursor) => {
      val hcursor =
        cursor.downField("data").downField("viewer").downField("repositories")
      val pageInfo = hcursor.downField("pageInfo")
      val maybeCursor =
        if (pageInfo.get[Boolean]("hasNextPage").getOrElse(false))
          pageInfo.get[String]("endCursor").toOption
        else None
      hcursor
        .get[List[Json]]("nodes")
        .map { jsonNodes =>
          val nodes = jsonNodes.flatMap { json =>
            val cursor = json.hcursor
            for {
              name <- cursor.get[String]("nameWithOwner").toOption
              url  <- cursor.get[String]("url").toOption
              branch <-
                cursor
                  .downField("defaultBranchRef")
                  .get[String]("name")
                  .toOption
            } yield Node(name, url, branch)
          }
          Repository(nodes, maybeCursor)
        }
    }
  }
  object GrahpQL {
    implicit val decoder: Decoder[GrahpQL] =
      List[Decoder[GrahpQL]](Decoder[Error].widen, Decoder[Repository].widen)
        .reduceLeft(_ or _)
  }
}
