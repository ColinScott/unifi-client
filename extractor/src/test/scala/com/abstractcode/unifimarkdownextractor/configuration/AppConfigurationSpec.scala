package com.abstractcode.unifimarkdownextractor.configuration

import java.nio.file.Path

import cats.data._
import cats.implicits._
import com.abstractcode.unificlient.ControllerConfiguration
import com.abstractcode.unificlient.ControllerConfiguration.UniFiCredentials
import com.abstractcode.unifimarkdownextractor.Arbitraries._
import com.abstractcode.unifimarkdownextractor.Generators
import com.abstractcode.unifimarkdownextractor.configuration.ParseError._
import org.http4s.Uri
import org.scalacheck.Prop.{forAll, propBoolean}
import org.scalacheck.{Arbitrary, Gen, Properties}

object AppConfigurationSpec extends Properties("AppConfiguration") {
  val badUsernameCredentials: Gen[UniFiCredentials] = for {
    username <- Generators.whitespaceString
    password <- Generators.nonEmptyOrWhitespaceString
  } yield UniFiCredentials(username, password)

  val badPasswordCredentials: Gen[UniFiCredentials] = for {
    username <- Generators.nonEmptyOrWhitespaceString
    password <- Generators.whitespaceString
  } yield UniFiCredentials(username, password)

  val entirelyBadCredentials: Gen[UniFiCredentials] = for {
    username <- Generators.whitespaceString
    password <- Generators.whitespaceString
  } yield UniFiCredentials(username, password)

  implicit val controllerConfiguration: Gen[ControllerConfiguration] = for {
    serverUri <-Generators.uri
    creds <- Generators.credentials
  } yield ControllerConfiguration(serverUri, creds)


  implicit val exportConfiguration: Gen[ExportConfiguration] = for {
    path <- Generators.path
  } yield ExportConfiguration(path)


  implicit val arbitraryAppConfiguration: Arbitrary[AppConfiguration] = Arbitrary {
    for {
      controller <- controllerConfiguration
      export <- exportConfiguration
    } yield AppConfiguration(controller, export)
  }

  def appConfigurationToMap(appConfiguration: AppConfiguration): Map[String, String] = Map(
    "SERVER_URI" -> appConfiguration.controller.serverUri.toString(),
    "USERNAME" -> appConfiguration.controller.credentials.username,
    "PASSWORD" -> appConfiguration.controller.credentials.password,
    "BASE_PATH" -> appConfiguration.export.basePath.toString
  )

  property("valid") = forAll {
    (appConfiguration: AppConfiguration) => {

      AppConfiguration.apply(appConfigurationToMap(appConfiguration)) == appConfiguration.validNel
    }
  }

  property("invalid uri") = forAll {
    (credentials: UniFiCredentials, path: Path) => {
      val env = Map(
        "SERVER_URI" -> "$#^$%&(%",
        "USERNAME" -> credentials.username,
        "PASSWORD" -> credentials.password,
        "BASE_PATH" -> path.toString
      )

      val expected = ParseError("SERVER_URI", InvalidFormat).invalidNel

      AppConfiguration(env) == expected
    }
  }

  property("missing uri") = forAll {
    (credentials: UniFiCredentials, path: Path) => {
      val env = Map(
        "USERNAME" -> credentials.username,
        "PASSWORD" -> credentials.password,
        "BASE_PATH" -> path.toString
      )

      val expected = ParseError("SERVER_URI", NotProvidedOrEmpty).invalidNel

      AppConfiguration(env) == expected
    }
  }

  property("bad username") = forAll(Generators.uri, badUsernameCredentials, Generators.path) {
    (uri: Uri, credentials: UniFiCredentials, path: Path) => {
      val env = Map(
        "SERVER_URI" -> uri.toString(),
        "USERNAME" -> credentials.username,
        "PASSWORD" -> credentials.password,
        "BASE_PATH" -> path.toString
      )

      val expected = ParseError("USERNAME", NotProvidedOrEmpty).invalidNel

      AppConfiguration(env) == expected
    }
  }

  property("bad password") = forAll(Generators.uri, badPasswordCredentials, Generators.path) {
    (uri: Uri, credentials: UniFiCredentials, path: Path) => {
      val env = Map(
        "SERVER_URI" -> uri.toString(),
        "USERNAME" -> credentials.username,
        "PASSWORD" -> credentials.password,
        "BASE_PATH" -> path.toString
      )

      val expected = ParseError("PASSWORD", NotProvidedOrEmpty).invalidNel

      AppConfiguration(env) == expected
    }
  }

  property("entirely bad credentials") = forAll(Generators.uri, entirelyBadCredentials, Generators.path) {
    (uri: Uri, credentials: UniFiCredentials, path: Path) => {
      val env = Map(
        "SERVER_URI" -> uri.toString(),
        "USERNAME" -> credentials.username,
        "PASSWORD" -> credentials.password,
        "BASE_PATH" -> path.toString
      )

      val expected: ValidatedNel[ParseError, AppConfiguration] = NonEmptyList.of(ParseError("USERNAME", NotProvidedOrEmpty), ParseError("PASSWORD", NotProvidedOrEmpty)).invalid

      AppConfiguration(env) == expected
    }
  }

  property("missing username") = forAll {
    (uri: Uri, credentials: UniFiCredentials, path: Path) => {
      val env = Map(
        "SERVER_URI" -> uri.toString(),
        "PASSWORD" -> credentials.password,
        "BASE_PATH" -> path.toString
      )

      val expected: ValidatedNel[ParseError, AppConfiguration] = ParseError("USERNAME", NotProvidedOrEmpty).invalidNel

      AppConfiguration(env) == expected
    }
  }

  property("missing password") = forAll {
    (uri: Uri, credentials: UniFiCredentials, path: Path) => {
      val env = Map(
        "SERVER_URI" -> uri.toString(),
        "USERNAME" -> credentials.username,
        "BASE_PATH" -> path.toString
      )

      val expected: ValidatedNel[ParseError, AppConfiguration] = ParseError("PASSWORD", NotProvidedOrEmpty).invalidNel

      AppConfiguration(env) == expected
    }
  }
}