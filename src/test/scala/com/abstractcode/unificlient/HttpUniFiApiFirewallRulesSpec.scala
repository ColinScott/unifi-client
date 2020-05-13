package com.abstractcode.unificlient

import cats.effect.IO
import com.abstractcode.unificlient.Arbitraries._
import com.abstractcode.unificlient.CommonChecks._
import com.abstractcode.unificlient.UniFiClient.UniFiAccess
import com.abstractcode.unificlient.models.FirewallRule._
import com.abstractcode.unificlient.models.Site._
import com.abstractcode.unificlient.models.UniFiResponse._
import com.abstractcode.unificlient.models.{FirewallRule, UniFiResponse}
import io.circe.Json
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{HttpRoutes, Status}
import org.scalacheck.Prop.{forAll, propBoolean}
import org.scalacheck.{Gen, Properties}

object HttpUniFiApiFirewallRulesSpec extends Properties("HttpUniFiApi firewall groups") {
  implicit val firewallRule: Gen[FirewallRule] = Generators.firewallRule

  property("get sites") = forAll {
    (siteName: SiteName, firewallRules: UniFiResponse[List[FirewallRule]]) => {
      val mockServer = HttpRoutes.of[IO] {
        case GET -> Root / "api" / "s" / siteName.name / "rest" / "firewallrule" => Ok(firewallRules.asJson)
      }.orNotFound

      val httpUniFiApp = new HttpUniFiClient[IO](Client.fromHttpApp(mockServer))

      httpUniFiApp.firewallRules(siteName).run(Fixture.fixedUniFiAccess).unsafeRunSync() == firewallRules.data
    }
  }

  property("send auth cookies") = forAll {
    (siteName: SiteName, uniFiAccess: UniFiAccess) => checkAuthCookies(
      uniFiAccess,
      Ok(Json.obj("data" -> Json.arr())),
      _.firewallRules(siteName)
    )
  }

  property("unauthorised") = forAll {
    (siteName: SiteName, uniFiAccess: UniFiAccess) => unauthorised(uniFiAccess, _.firewallRules(siteName))
  }

  property("unexpected status code") = forAll {
    (siteName: SiteName, status: Status) => unexpectedStatus(status, _.firewallRules(siteName))
  }

  property("invalid response body") = forAll {
    (siteName: SiteName, notJson: String) => invalidResponseBody(notJson, _.firewallRules(siteName))
  }
}
