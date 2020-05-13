package restui

import java.util.{Iterator => JIterator}
import scala.collection.Iterator
import scala.collection.JavaConverters.asScalaIterator

package object servicediscovery {
  implicit class IteratorOps[T](val iterator: JIterator[T]) extends AnyVal {
    def asScala: Iterator[T] = asScalaIterator(iterator)
  }
}
