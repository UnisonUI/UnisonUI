package restui

import java.util.{Iterator => JIterator}
import scala.collection.Iterator
import scala.jdk.CollectionConverters.IteratorHasAsScala

package object servicediscovery {
  implicit class IteratorOps[T](val iterator: JIterator[T]) extends AnyVal {
    def asScala: Iterator[T] = IteratorHasAsScala(iterator).asScala
  }
}
