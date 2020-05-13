package com.abstractcode.unificlient

import cats.effect.IO
import com.abstractcode.unificlient.Arbitraries._
import com.abstractcode.unificlient.UniFiClient.UniFiAccess
import com.abstractcode.unificlient.CommonChecks._
import io.circe.Json
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{HttpRoutes, Status}
import org.scalacheck.Prop.{forAll, propBoolean}
import org.scalacheck.Properties

object HttpUniFiApiLogoutSpec extends Properties("HttpUniFiApi authentication") {

  property("successful logout") = forAll {
    (uniFiAccess: UniFiAccess) => {
      val mockServer = HttpRoutes.of[IO] {
        case POST -> Root / "api" / "logout" => Ok(Json.obj("data" -> Json.arr()))
      }.orNotFound

      val httpUniFiApp = new HttpUniFiClient[IO](Client.fromHttpApp(mockServer))

      httpUniFiApp.logout().run(uniFiAccess).attempt.unsafeRunSync() == Right(())
    }
  }

  property("send auth cookies") = forAll {
    (uniFiAccess: UniFiAccess) => checkAuthCookies(
      uniFiAccess,
      Ok(Json.obj("data" -> Json.arr())),
      _.logout()
    )
  }

  property("unauthorised") = forAll {
    (uniFiAccess: UniFiAccess) => unauthorised(uniFiAccess, _.logout())
  }

  property("unexpected status code") = forAll {
    (status: Status) => unexpectedStatus(status, _.logout())
  }
}
