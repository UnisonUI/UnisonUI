package restui.providers.docker.client

import scala.concurrent.Future

import akka.NotUsed
import akka.http.scaladsl.model.{HttpResponse, Uri}
import akka.stream.scaladsl.Source

trait HttpClient {
  def get(path: Uri): Future[HttpResponse]
  def watch(path: Uri): Source[HttpResponse, NotUsed]
  def downloadFile(uri: String): Future[String]
}
