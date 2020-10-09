package restui.protobuf.marshal

object Errors {
  final case class RequiredField(name: String) extends Exception(s"$name is required")
}

