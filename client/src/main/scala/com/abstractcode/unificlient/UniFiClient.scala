package com.abstractcode.unificlient

import cats.data.Kleisli
import cats.effect.Sync
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

class HttpUniFiClient[F[_]: Sync](client: Client[F]) extends UniFiClient[F] {
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

          Sync[F].fromOption(uniFiSes.map2(csrfToken)(AuthCookies), InvalidAuthenticationResponse)
            .map(cookies => UniFiAccess(cookies, configuration.serverUri))
        case _ => Sync[F].raiseError[UniFiAccess](AuthenticationFailure(response.status))
      }
    }
  })

  def logout(): UniFiRequest[F, Unit] = handleWithAuthentication(
    uri => Request[F](
      method = Method.POST,
      uri = uri / "api" / "logout"
    ),
    _.as[UniFiResponse[Unit]]
  )

  def sites(): UniFiRequest[F, List[Site]] = handleWithAuthentication(
    uri => Request[F](
      method = Method.GET,
      uri = uri / "api" / "self" / "sites"
    ),
    _.as[UniFiResponse[List[Site]]]
  )

  def networks(name: SiteName): UniFiRequest[F, List[Network]] = handleWithAuthentication(
    uri => Request[F](
      method = Method.GET,
      uri = uri / "api" / "s" / name.name / "rest" / "networkconf"
    ),
    _.as[UniFiResponse[List[Network]]]
  )

  def firewallGroups(name: SiteName): UniFiRequest[F, List[FirewallGroup]] = handleWithAuthentication(
    uri => Request[F](
      method = Method.GET,
      uri = uri / "api" / "s" / name.name / "rest" / "firewallgroup"
    ),
    _.as[UniFiResponse[List[FirewallGroup]]]
  )

  def firewallRules(name: SiteName): UniFiRequest[F, List[FirewallRule]] = handleWithAuthentication(
    uri => Request[F](
      method = Method.GET,
      uri = uri / "api" / "s" / name.name / "rest" / "firewallrule"
    ),
    _.as[UniFiResponse[List[FirewallRule]]]
  )

  def handleWithAuthentication[R](
    request: Uri => Request[F],
    success: Response[F] => F[UniFiResponse[R]]
  ): UniFiRequest[F, R] = Kleisli (access => {
    client.run(request(access.serverUri).addAuthCookies(access.authCookies)).use { response =>
      response.status.responseClass match {
        case Successful => success(response).map(_.data).handleErrorWith(e => Sync[F].raiseError(InvalidResponse(e)))
        case ClientError if response.status.code == 401 => Sync[F].raiseError[R](TokenUnauthorised)
        case _ => Sync[F].raiseError[R](UniFiError(response.status))
      }
    }
  })
}


