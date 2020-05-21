package restui.mocks

import java.{util => ju}

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.EventsCmd
import com.github.dockerjava.api.model.Event

class EventsCmdMock(maybeEvent: Option[Event] = None) extends EventsCmd {
  def close(): Unit = ???

  def exec[T <: ResultCallback[Event]](x: T): T = {
    maybeEvent.foreach(x.onNext)
    x
  }

  def getFilters(): ju.Map[String, ju.List[String]] = ???

  def getSince(): String = ???

  def getUntil(): String = ???

  def withContainerFilter(x: String*): EventsCmd = this

  def withEventFilter(x: String*): EventsCmd = this

  def withImageFilter(x: String*): EventsCmd = this

  def withLabelFilter(x: String*): EventsCmd = this

  def withLabelFilter(x: ju.Map[String, String]): EventsCmd = this

  def withSince(x: String): EventsCmd = this

  def withUntil(x: String): EventsCmd = this

}
