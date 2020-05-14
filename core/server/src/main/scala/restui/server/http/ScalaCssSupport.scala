package restui.server.http

import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.marshalling._

import scalacss.DevDefaults._
import scalacss.internal.mutable.StyleSheet

trait ScalaCssSupport {
  implicit def scalaCssMarshaller: ToEntityMarshaller[StyleSheet.Base] =
    Marshaller.StringMarshaller.wrap(MediaTypes.`text/css`)(_.render)
}
