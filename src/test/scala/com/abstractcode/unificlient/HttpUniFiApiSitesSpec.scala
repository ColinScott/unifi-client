package com.abstractcode.unificlient

import cats.effect.IO
import com.abstractcode.unificlient.Arbitraries._
import com.abstractcode.unificlient.CommonChecks._
import com.abstractcode.unificlient.UniFiClient.UniFiAccess
import com.abstractcode.unificlient.models.Site._
import com.abstractcode.unificlient.models.{Site, UniFiResponse}
import io.circe.Json
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{HttpRoutes, Status}
import org.scalacheck.Prop.{forAll, propBoolean}
import org.scalacheck.{Gen, Properties}

object HttpUniFiApiSitesSpec extends Properties("HttpUniFiApi sites") {
  implicit val site: Gen[Site] = Generators.site

  property("get sites") = forAll {
    (sites: UniFiResponse[List[Site]]) => {
      val mockServer = HttpRoutes.of[IO] {
        case GET -> Root / "api" / "self" / "sites" => Ok(sites.asJson)
      }.orNotFound

      val httpUniFiApp = new HttpUniFiClient[IO](Client.fromHttpApp(mockServer))

      httpUniFiApp.sites().run(Fixture.fixedUniFiAccess).unsafeRunSync() == sites.data
    }
  }

  property("send auth cookies") = forAll {
    (uniFiAccess: UniFiAccess) => checkAuthCookies(
      uniFiAccess,
      Ok(Json.obj("data" -> Json.arr())),
      _.sites()
    )
  }

  property("unauthorised") = forAll {
    (uniFiAccess: UniFiAccess) => unauthorised(uniFiAccess, _.sites())
  }

  property("unexpected status code") = forAll {
    (status: Status) => unexpectedStatus(status, _.sites())
  }

  property("invalid response body") = forAll {
    (notJson: String) => invalidResponseBody(notJson, _.sites())
  }
}
