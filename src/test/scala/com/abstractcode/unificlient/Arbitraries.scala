package com.abstractcode.unificlient

import java.nio.file.Path

import com.abstractcode.unificlient.ControllerConfiguration.UniFiCredentials
import com.abstractcode.unificlient.Generators.uri
import com.abstractcode.unificlient.UniFiClient.UniFiAccess
import com.abstractcode.unificlient.models.Network.NetworkName
import com.abstractcode.unificlient.models.Site.SiteName
import com.abstractcode.unificlient.models._
import org.http4s.Status.{Forbidden, GatewayTimeout, Gone, NotImplemented, UpgradeRequired, VariantAlsoNegotiates}
import org.http4s.{Status, Uri}
import org.scalacheck.{Arbitrary, Gen}

object Arbitraries {
  implicit val arbitraryUri: Arbitrary[Uri] = Arbitrary(uri)
  implicit val arbitraryCredentials: Arbitrary[UniFiCredentials] = Arbitrary(Generators.credentials)

  implicit val arbitraryControllerConfiguration: Arbitrary[ControllerConfiguration] = Arbitrary {
    for {
      uri <- Generators.uri
      credentials <- Generators.credentials
    } yield ControllerConfiguration(uri, credentials)
  }

  implicit val arbitraryAuthCookies: Arbitrary[AuthCookies] = Arbitrary(Generators.authCookies)
  implicit val arbitraryUniFiAccess: Arbitrary[UniFiAccess] = Arbitrary(Generators.uniFiAccess)

  implicit val arbitrarySiteId: Arbitrary[SiteId] = Arbitrary(Generators.siteId)
  implicit val arbitrarySiteName: Arbitrary[SiteName] = Arbitrary(Generators.siteName)
  implicit val arbitrarySitesDetailsSite: Arbitrary[Site] = Arbitrary(Generators.site)

  implicit def arbitraryUniFiResponse[T](implicit tGen: Gen[T]): Arbitrary[UniFiResponse[List[T]]] = Arbitrary {
    for {
      t <- Gen.listOf(tGen)
    } yield UniFiResponse(t)
  }

  implicit val arbitraryStatus: Arbitrary[Status] = Arbitrary {
    Gen.oneOf(
      Forbidden,
      Gone,
      NotImplemented,
      GatewayTimeout,
      VariantAlsoNegotiates,
      UpgradeRequired
    )
  }

  implicit val arbitraryNetworkId: Arbitrary[NetworkId] = Arbitrary(Generators.networkId)
  implicit val arbitraryNetworkName: Arbitrary[NetworkName] = Arbitrary(Generators.networkName)
  implicit val arbitraryNetwork: Arbitrary[Network] = Arbitrary(Gen.oneOf(Generators.defaultNetwork, Generators.lan, Generators.wan))

  implicit val arbitraryIpAddressV4: Arbitrary[IpAddressV4] = Arbitrary(Generators.ipAddressV4)
  implicit val arbitraryCidrV4: Arbitrary[CidrV4] = Arbitrary(Generators.cidrV4)

  implicit val arbitraryPath: Arbitrary[Path] = Arbitrary(Generators.path)

  implicit val arbitraryFirewallGroup: Arbitrary[FirewallGroup] = Arbitrary(Generators.firewallGroup)
  implicit val arbitraryFirewallRule: Arbitrary[FirewallRule] = Arbitrary(Generators.firewallRule)
}
