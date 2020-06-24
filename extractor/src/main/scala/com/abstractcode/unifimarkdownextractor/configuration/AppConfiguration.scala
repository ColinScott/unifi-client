package com.abstractcode.unifimarkdownextractor.configuration

import java.nio.file.{Path, Paths}

import cats.Show
import cats.data.ValidatedNel
import cats.implicits._
import com.abstractcode.unificlient.ControllerConfiguration
import com.abstractcode.unificlient.ControllerConfiguration.UniFiCredentials
import com.abstractcode.unifimarkdownextractor.configuration.ParseError._
import org.http4s.Uri

case class AppConfiguration(controller: ControllerConfiguration, export: ExportConfiguration)

case class ParseError(environmentVariable: String, reason: Reason)

object ParseError {
  sealed trait Reason
  case object NotProvidedOrEmpty extends Reason
  case object InvalidFormat extends Reason

  implicit val showParseError: Show[ParseError] = Show.show {
    case ParseError(variable, NotProvidedOrEmpty) => s"Environment variable $variable was not provided or is empty"
    case ParseError(variable, InvalidFormat) => s"Format of environment variable $variable is invalid"
  }
}

case class ExportConfiguration(basePath: Path)

object AppConfiguration {
  def apply(env: Map[String, String]): ValidatedNel[ParseError, AppConfiguration] = (
    getControllerConfiguration(env),
    getExportConfiguration(env)
    ).mapN(AppConfiguration.apply)

  def getControllerConfiguration(env: Map[String, String]): ValidatedNel[ParseError, ControllerConfiguration] = (
    getUri(env, "SERVER_URI"),
    getCredentials(env)
    ).mapN(ControllerConfiguration.apply)

  def getUri(env: Map[String, String], key: String): ValidatedNel[ParseError, Uri] =
    env.get(key)
      .map(e => Uri.fromString(e).leftMap(_ => ParseError(key, InvalidFormat)).toValidatedNel)
      .getOrElse(ParseError(key, NotProvidedOrEmpty).invalidNel)

  def getNonEmptyString(env: Map[String, String], key: String): ValidatedNel[ParseError, String] = env.get(key)
    .filter(!_.trim.isEmpty)
    .toValidNel(ParseError(key, NotProvidedOrEmpty))

  def getCredentials(env: Map[String, String]): ValidatedNel[ParseError, UniFiCredentials] = (
    getNonEmptyString(env, "USERNAME"),
    getNonEmptyString(env, "PASSWORD")
    ).mapN(UniFiCredentials)

  def getExportConfiguration(env: Map[String, String]): ValidatedNel[ParseError, ExportConfiguration] =
    getNonEmptyString(env, "BASE_PATH").map(p => ExportConfiguration(Paths.get(p)))
}