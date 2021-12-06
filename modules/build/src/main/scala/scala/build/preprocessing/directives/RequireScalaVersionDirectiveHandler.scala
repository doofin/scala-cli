package scala.build.preprocessing.directives

import os.Path

import scala.build.errors.{BuildException, DirectiveErrors}
import scala.build.options.BuildRequirements
import scala.build.preprocessing.{ScopePath, Scoped}

case object RequireScalaVersionDirectiveHandler extends RequireDirectiveHandler {
  def name             = "Scala version"
  def description      = "Require a Scala version for the current file"
  def usage            = "// using target.scala _version_"
  override def usageMd = "`// using target.scala `_version_"
  override def examples = Seq(
    "// using target.scala \"3\"",
    "// using target.scala.>= \"2.13\"",
    "// using target.scala.< \"3.0.2\""
  )

  override def keys: Seq[String] = Seq(
    "target.scala.==",
    "target.scala.>=",
    "target.scala.<=",
    "target.scala.>",
    "target.scala.<",
    "target.scala"
  )

  def handle(
    directive: Directive,
    cwd: ScopePath
  ): Option[Either[BuildException, BuildRequirements]] =
    directive.values match {
      case Seq("scala", ">=", minVer) =>
        val req = BuildRequirements(
          scalaVersion = Seq(BuildRequirements.VersionHigherThan(minVer, orEqual = true))
        )
        Some(Right(req))
      case Seq("scala", "<=", maxVer) =>
        val req = BuildRequirements(
          scalaVersion = Seq(BuildRequirements.VersionLowerThan(maxVer, orEqual = true))
        )
        Some(Right(req))
      case Seq("scala", "==", reqVer) =>
        // FIXME What about things like just '2.12'?
        val req = BuildRequirements(
          scalaVersion = Seq(BuildRequirements.VersionEquals(reqVer, loose = true))
        )
        Some(Right(req))
      case _ =>
        None
    }

  private def handleVersion(
    key: String,
    v: String
  ): Either[BuildException, Option[BuildRequirements]] = key match {
    case "target.scala" | "target.scala.==" =>
      val req = BuildRequirements(
        scalaVersion = Seq(BuildRequirements.VersionEquals(v, loose = true))
      )
      Right(Some(req))
    case "target.scala.>" =>
      val req = BuildRequirements(
        scalaVersion = Seq(BuildRequirements.VersionHigherThan(v, orEqual = false))
      )
      Right(Some(req))
    case "target.scala.<" =>
      val req = BuildRequirements(
        scalaVersion = Seq(BuildRequirements.VersionLowerThan(v, orEqual = false))
      )
      Right(Some(req))
    case "target.scala.>=" =>
      val req = BuildRequirements(
        scalaVersion = Seq(BuildRequirements.VersionHigherThan(v, orEqual = true))
      )
      Right(Some(req))
    case "target.scala.<=" =>
      val req = BuildRequirements(
        scalaVersion = Seq(BuildRequirements.VersionLowerThan(v, orEqual = true))
      )
      Right(Some(req))
    case _ =>
      // TODO: Handle errors and conflicts
      Left(new DirectiveErrors(::("Match error in ScalaVersionDirectiveHandler", Nil)))
  }

  override def handleValues(
    directive: StrictDirective,
    path: Either[String, Path],
    cwd: ScopePath
  ): Either[BuildException, (Option[BuildRequirements], Seq[Scoped[BuildRequirements]])] = {
    val values         = DirectiveUtil.stringValues(directive.values, path, cwd)
    val nonscopedValue = values.find(_._3.isEmpty).map(_._1)
    val nonscoped =
      nonscopedValue.fold[Either[BuildException, Option[BuildRequirements]]](Right(None))(v =>
        handleVersion(directive.key, v)
      )
    val scoped = values.filter(_._3.nonEmpty).map {
      case (v, _, Some(scopePath)) =>
        handleVersion(directive.key, v).map(_.map(req => Scoped(scopePath, req)))
    }.foldLeft[Either[BuildException, Seq[Scoped[BuildRequirements]]]](Right(Seq.empty)) {
      case (Right(seq), Right(v)) => Right(seq ++ v)
      case (Left(err), _)         => Left(err)
      case (_, Left(err))         => Left(err)
    }
    for {
      ns <- nonscoped
      s  <- scoped
    } yield ns -> s
  }
}
