package com.abstractcode.unificlient

import com.abstractcode.unificlient.ControllerConfiguration.UniFiCredentials
import org.http4s.Uri

case class ControllerConfiguration(
  serverUri: Uri,
  credentials: UniFiCredentials
)

object ControllerConfiguration {
  case class UniFiCredentials(username: String, password: String)
}
