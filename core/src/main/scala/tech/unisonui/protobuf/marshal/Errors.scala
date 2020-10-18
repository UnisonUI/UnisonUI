package tech.unisonui.protobuf.marshal

object Errors {
  final case class InvalidField(id: Int) extends Exception(s"invalid field $id")
  final case class InvalidEnumEntry(id: Int)
      extends Exception(s"invalid enum entry $id")
  final case class RequiredField(name: String)
      extends Exception(s"$name is required")
}
