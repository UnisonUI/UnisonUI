package restui.servicediscovery.git.github

import akka.NotUsed
import akka.stream.scaladsl.Flow
import restui.servicediscovery.git.git.Repo
import restui.servicediscovery.git.settings.GitHub

object GithubFlow {

  val flow: Flow[GitHub, Repo, NotUsed] =
    Flow[GitHub]
      .map(_ => ???)
      .async
}
