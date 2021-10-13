package scala.build.preprocessing.directives

import scala.build.Position
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScalaNativeOptions}
import scala.build.preprocessing.ScopePath

case object UsingScalaNativeOptionsDirectiveHandler extends UsingDirectiveHandler {
  def name: String = "Scala Native options"

  def description: String = "Add Scala Native options"

  def usage: String = "Scala Native options"

  override def usageMd: String = "directive using usage"

  override def examples: Seq[String] = Seq()
  def handle(directive: Directive, cwd: ScopePath): Option[Either[BuildException, BuildOptions]] =
    directive.values match {
      case Seq(param @ "native-gc", value @ _*) =>
        if (value.size == 1) {
          val options = BuildOptions(
            scalaNativeOptions = ScalaNativeOptions(
              gcStr = Some(value.head)
            )
          )
          Some(Right(options))
        }
        else Some(Left(SingleValueExpected(param, value)))

      case _ =>
        None

    }

  override def keys: Seq[String] =
    Seq("native-gc")

  override def handleValues(
    values: Seq[Any],
    cwd: ScopePath,
    positionOpt: Option[Position]
  ): Either[BuildException, BuildOptions] = {
    val scalaNativeOptions = DirectiveUtil.stringValues(values).toList
    val options = BuildOptions(
      scalaNativeOptions = ScalaNativeOptions(
        compileOptions = scalaNativeOptions
      )
    )
    Right(options)
  }

}

final case class SingleValueExpected(param: String, values: Seq[String]) extends BuildException(
      s"Expected single value for $param but found $values"
    )