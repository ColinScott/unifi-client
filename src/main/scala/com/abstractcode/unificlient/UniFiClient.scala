package com.abstractcode.unificlient

import cats.ApplicativeError
import cats.data.Kleisli
import cats.effect._
import cats.implicits._
import com.abstractcode.unificlient.AddAuthCookies._
import com.abstractcode.unificlient.Error._
import com.abstractcode.unificlient.UniFiClient.{UniFiAccess, UniFiRequest}
import com.abstractcode.unificlient.models.Site.SiteName
import com.abstractcode.unificlient.models._
import fs2.Stream
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.Status.{ClientError, Successful}
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s._

trait UniFiClient[F[_]] {
  def authenticate(): Kleisli[F, ControllerConfiguration, UniFiAccess]
  def logout(): UniFiRequest[F, Unit]

  def sites(): UniFiRequest[F, List[Site]]
  def networks(name: SiteName): UniFiRequest[F, List[Network]]
  def firewallGroups(name: SiteName): UniFiRequest[F, List[FirewallGroup]]
  def firewallRules(name: SiteName): UniFiRequest[F, List[FirewallRule]]
}

object UniFiClient {
  case class UniFiAccess(authCookies: AuthCookies, serverUri: Uri)

  type UniFiRequest[F[_], R] = Kleisli[F, UniFiAccess, R]
}

class HttpUniFiClient[F[_] : Sync](client: Client[F])(implicit monadError: ApplicativeError[F, Throwable]) extends UniFiClient[F] {
  def authenticate(): Kleisli[F, ControllerConfiguration, UniFiAccess] = Kleisli(configuration => {
    def getCookieValue(cookies: List[ResponseCookie])(name: String): Option[String] = cookies.filter(_.name == name)
      .map(_.content)
      .headOption

    val postRequest = Request[F](
      method = Method.POST,
      uri = configuration.serverUri / "api" / "login",
      body = Stream.emits[F, Byte](configuration.credentials.asJson.noSpaces.getBytes)
    )

    client.run(postRequest).use { response =>
      response.status.responseClass match {
        case Successful =>
          val getCookies: String => Option[String] = getCookieValue(response.cookies)
          val uniFiSes = getCookies("unifises")
          val csrfToken = getCookies("csrf_token")

          monadError.fromOption(uniFiSes.map2(csrfToken)(AuthCookies), InvalidAuthenticationResponse)
            .map(cookies => UniFiAccess(cookies, configuration.serverUri))
        case _ => monadError.raiseError[UniFiAccess](AuthenticationFailure(response.status))
      }
    }
  })

  def logout(): UniFiRequest[F, Unit] = Kleisli(access => {
    val request: Request[F] = Request[F](
      method = Method.POST,
      uri = access.serverUri / "api" / "logout"
    )

    handleWithAuthentication(request, access.authCookies, _.as[UniFiResponse[Unit]])
  })

    def sites():  UniFiRequest[F, List[Site]] = Kleisli(access => {
    val request: Request[F] = Request[F](
      method = Method.GET,
      uri = access.serverUri / "api" / "self" / "sites"
    )

    handleWithAuthentication(
      request,
      access.authCookies,
      _.as[UniFiResponse[List[Site]]]
    )
  })

  def networks(name: SiteName): UniFiRequest[F, List[Network]] = Kleisli(access => {
    val request: Request[F] = Request[F](
      method = Method.GET,
      uri = access.serverUri / "api" / "s" / name.name / "rest" / "networkconf"
    )

    handleWithAuthentication(
      request,
      access.authCookies,
      _.as[UniFiResponse[List[Network]]]
    )
  })

  def firewallGroups(name: SiteName): UniFiRequest[F, List[FirewallGroup]] = Kleisli(access => {
    val request: Request[F] = Request[F](
      method = Method.GET,
      uri = access.serverUri / "api" / "s" / name.name / "rest" / "firewallgroup"
    )

    handleWithAuthentication(
      request,
      access.authCookies,
      _.as[UniFiResponse[List[FirewallGroup]]]
    )
  })

  def firewallRules(name: SiteName): UniFiRequest[F, List[FirewallRule]] = Kleisli(access => {
    val request: Request[F] = Request[F](
      method = Method.GET,
      uri = access.serverUri / "api" / "s" / name.name / "rest" / "firewallrule"
    )

    handleWithAuthentication(
      request,
      access.authCookies,
      _.as[UniFiResponse[List[FirewallRule]]]
    )
  })

  def handleWithAuthentication[R](
    request: Request[F],
    authCookies: AuthCookies,
    success: Response[F] => F[UniFiResponse[R]]
  ): F[R] = {
    client.run(request.addAuthCookies(authCookies)).use { response =>
      response.status.responseClass match {
        case Successful => success(response).map(_.data).handleErrorWith(e => monadError.raiseError(InvalidResponse(e)))
        case ClientError if response.status.code == 401 => monadError.raiseError[R](TokenUnauthorised)
        case _ => monadError.raiseError[R](UniFiError(response.status))
      }
    }
  }
}


