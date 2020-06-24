package com.abstractcode.unificlient

import cats.data.NonEmptyList
import cats.effect.IO
import com.abstractcode.unificlient.Error.{InvalidResponse, TokenUnauthorised, UniFiError}
import com.abstractcode.unificlient.UniFiClient.{UniFiAccess, UniFiRequest}
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s._
import org.scalacheck.Prop
import org.scalacheck.Prop.propBoolean

object CommonChecks {
  def checkAuthCookies[R](uniFiAccess: UniFiAccess, validResponse: IO[Response[IO]], apiMethod: UniFiClient[IO] => UniFiRequest[IO, R]): Prop = {
    val expectedCookies = Set(
      RequestCookie("unifises", uniFiAccess.authCookies.uniFiSes),
      RequestCookie("csrf_token", uniFiAccess.authCookies.csrfToken)
    )
    val mockServer = HttpRoutes.of[IO] {
      case req@ _ =>
        if (req.cookies.toSet == expectedCookies)
          validResponse
        else
          InternalServerError()
    }.orNotFound

    val httpUniFiApp = new HttpUniFiClient[IO](Client.fromHttpApp(mockServer))

    propBoolean(apiMethod(httpUniFiApp).run(uniFiAccess).attempt.unsafeRunSync().isRight)
  }

  def unauthorised[R](uniFiAccess: UniFiAccess, apiMethod: UniFiClient[IO] => UniFiRequest[IO, R]): Prop = {
    val mockServer = HttpRoutes.of[IO] {
      case _ => Unauthorized(
        headers.`WWW-Authenticate`(NonEmptyList.of(Challenge("Basic", "test", Map.empty)))
      )
    }.orNotFound

    val httpUniFiApp = new HttpUniFiClient[IO](Client.fromHttpApp(mockServer))

    propBoolean(apiMethod(httpUniFiApp).run(uniFiAccess).attempt.unsafeRunSync() == Left(TokenUnauthorised))
  }

  def unexpectedStatus[R](status: Status, apiMethod: UniFiClient[IO] => UniFiRequest[IO, R]): Prop = {
    val mockServer = HttpRoutes.of[IO] {
      case _ => IO.pure(Response[IO](status = status))
    }.orNotFound

    val httpUniFiApp = new HttpUniFiClient[IO](Client.fromHttpApp(mockServer))

    propBoolean(apiMethod(httpUniFiApp).run(Fixture.fixedUniFiAccess).attempt.unsafeRunSync() == Left(UniFiError(status)))
  }

  def invalidResponseBody[R](notJson: String, apiMethod: UniFiClient[IO] => UniFiRequest[IO, R]) : Prop = {
    val mockServer = HttpRoutes.of[IO] {
      case _ => Ok(notJson)
    }.orNotFound

    val httpUniFiApp = new HttpUniFiClient[IO](Client.fromHttpApp(mockServer))

    propBoolean(apiMethod(httpUniFiApp).run(Fixture.fixedUniFiAccess).attempt
      .unsafeRunSync() match {
      case Left(InvalidResponse(_)) => true
      case _ => false
    })
  }
}
