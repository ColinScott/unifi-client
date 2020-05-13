package com.abstractcode.unificlient

import com.abstractcode.unificlient.Error._
import org.http4s.Status

sealed trait Error extends Throwable {
  override def getMessage: String = this match {
    case AuthenticationFailure(status) => s"Authentication failure calling UniFi controller. Received status ${status.code}"
    case InvalidAuthenticationResponse => "The response to the authentication request couldn't be parsed"
    case TokenUnauthorised => "The authentication token was not accepted by the UniFi controller. It may have expired."
    case UniFiError(status) => s"Something broke calling the UniFi controller. Received status ${status.code}"
    case InvalidResponse(error) => s"The response from the UniFi controller could not be parsed.\n${error.getMessage}"
  }
}

object Error {

  case class AuthenticationFailure(status: Status) extends Error
  case object InvalidAuthenticationResponse extends Error
  case object TokenUnauthorised extends Error
  case class UniFiError(status: Status) extends Error
  case class InvalidResponse(error: Throwable) extends Error
}