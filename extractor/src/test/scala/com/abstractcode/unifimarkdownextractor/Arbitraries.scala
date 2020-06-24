package com.abstractcode.unifimarkdownextractor

import java.nio.file.Path

import com.abstractcode.unificlient.ControllerConfiguration
import com.abstractcode.unificlient.ControllerConfiguration.UniFiCredentials
import com.abstractcode.unifimarkdownextractor.Generators.uri
import org.http4s.Uri
import org.scalacheck.Arbitrary

object Arbitraries {
  implicit val arbitraryUri: Arbitrary[Uri] = Arbitrary(uri)
  implicit val arbitraryCredentials: Arbitrary[UniFiCredentials] = Arbitrary(Generators.credentials)

  implicit val arbitraryControllerConfiguration: Arbitrary[ControllerConfiguration] = Arbitrary {
    for {
      uri <- Generators.uri
      credentials <- Generators.credentials
    } yield ControllerConfiguration(uri, credentials)
  }

  implicit val arbitraryPath: Arbitrary[Path] = Arbitrary(Generators.path)
}
