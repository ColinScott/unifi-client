package com.abstractcode.unificlient

import cats.effect.IO
import com.abstractcode.unificlient.Arbitraries._
import com.abstractcode.unificlient.CommonChecks._
import com.abstractcode.unificlient.UniFiClient.UniFiAccess
import com.abstractcode.unificlient.models.FirewallGroup._
import com.abstractcode.unificlient.models.Site._
import com.abstractcode.unificlient.models.UniFiResponse._
import com.abstractcode.unificlient.models.{FirewallGroup, UniFiResponse}
import io.circe.Json
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{HttpRoutes, Status}
import org.scalacheck.Prop.{forAll, propBoolean}
import org.scalacheck.{Gen, Properties}

object HttpUniFiApiFirewallGroupsSpec extends Properties("HttpUniFiApi firewall groups") {
  implicit val firewallGroup: Gen[FirewallGroup] = Generators.firewallGroup

  property("get sites") = forAll {
    (siteName: SiteName, firewallGroups: UniFiResponse[List[FirewallGroup]]) => {
      val mockServer = HttpRoutes.of[IO] {
        case GET -> Root / "api" / "s" / siteName.name / "rest" / "firewallgroup" => Ok(firewallGroups.asJson)
      }.orNotFound

      val httpUniFiApp = new HttpUniFiClient[IO](Client.fromHttpApp(mockServer))

      httpUniFiApp.firewallGroups(siteName).run(Fixture.fixedUniFiAccess).unsafeRunSync() == firewallGroups.data
    }
  }

  property("send auth cookies") = forAll {
    (siteName: SiteName, uniFiAccess: UniFiAccess) => checkAuthCookies(
      uniFiAccess,
      Ok(Json.obj("data" -> Json.arr())),
      _.firewallGroups(siteName)
    )
  }

  property("unauthorised") = forAll {
    (siteName: SiteName, uniFiAccess: UniFiAccess) => unauthorised(uniFiAccess, _.firewallGroups(siteName))
  }

  property("unexpected status code") = forAll {
    (siteName: SiteName, status: Status) => unexpectedStatus(status, _.firewallGroups(siteName))
  }

  property("invalid response body") = forAll {
    (siteName: SiteName, notJson: String) => invalidResponseBody(notJson, _.firewallGroups(siteName))
  }
}
