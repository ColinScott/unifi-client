package com.abstractcode.unificlient

import java.nio.file.{FileSystems, Path}

import com.abstractcode.unificlient.ControllerConfiguration.UniFiCredentials
import com.abstractcode.unificlient.UniFiClient.UniFiAccess
import com.abstractcode.unificlient.models.FirewallGroup.{Ipv4AddressSubnetGroup, PortGroup, UnknownFirewallGroup}
import com.abstractcode.unificlient.models.Network.{NetworkName, VLan}
import com.abstractcode.unificlient.models.Site.SiteName
import com.abstractcode.unificlient.models._
import org.http4s.Uri
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.Gen._

object Generators {
  val one = 1 // Work around Scala 2.13.2 bug
  val nonEmptyString: Gen[String] = arbitrary[String].suchThat(!_.isEmpty)
  val nonEmptyOrWhitespaceString: Gen[String] = arbitrary[String].suchThat(!_.trim.isEmpty)
  val whitespaceString: Gen[String] = Gen.chooseNum(one - 1, 32).map(" " * _)
  val hiddenId: Gen[Option[String]] = Gen.option(Gen.identifier)
  val noDelete: Gen[Option[Boolean]] = Gen.option(Gen.oneOf(List(true, false)))

  val authCookies: Gen[AuthCookies] = for {
    uniFiSes <- Gen.alphaStr
    csrfToken <- Gen.alphaStr
  } yield AuthCookies(uniFiSes, csrfToken)

  val uri: Gen[Uri] = for {
    protocol <- Gen.frequency(List((5, "http://"), (10, "https://")).map(freqTuple): _*)
    uri <- Gen.identifier
    port <- Gen.chooseNum[Int](minT = 1, maxT = 65535)
  } yield Uri.unsafeFromString(s"$protocol$uri:$port")

  val uniFiAccess: Gen[UniFiAccess] = for {
    cookies <- authCookies
    serverUri <- uri
  } yield UniFiAccess(cookies, serverUri)

  val credentials: Gen[UniFiCredentials] = for {
    username <- nonEmptyOrWhitespaceString
    password <- nonEmptyOrWhitespaceString
  } yield UniFiCredentials(username, password)

  val siteId: Gen[SiteId] = for {
    id <- Gen.identifier
  } yield SiteId(id)

  val siteName: Gen[SiteName] = for {
    id <- Gen.identifier
  } yield SiteName(id)

  val site: Gen[Site] = for {
    id <- siteId
    name <- siteName
    description <- Gen.identifier
    role <- Gen.identifier
    hiddenId <- hiddenId
    delete <- noDelete
  } yield Site(id, name, description, role, hiddenId, delete)

  val networkId: Gen[NetworkId] = for {
    id <- Gen.identifier
  } yield NetworkId(id)

  val networkName: Gen[NetworkName] = for {
    id <- Gen.identifier
  } yield NetworkName(id)

  val ipAddressV4: Gen[IpAddressV4] = for {
    a <- arbitrary[Byte]
    b <- arbitrary[Byte]
    c <- arbitrary[Byte]
    d <- arbitrary[Byte]
  } yield IpAddressV4(a, b, c, d)

  val cidrV4: Gen[CidrV4] = for {
    ip <- ipAddressV4
    prefixLength <- Gen.choose[Byte]((one - 1).toByte, 32.toByte)
  } yield CidrV4(ip, prefixLength)

  val defaultNetwork: Gen[LocalNetwork] = for {
    id <- networkId
    name <- networkName
    ipSubnet <- cidrV4
  } yield LocalNetwork(id, name, None, ipSubnet, Some("LAN"), Some(true))

  val lan: Gen[LocalNetwork] = for {
    id <- networkId
    name <- networkName
    vlan <- Gen.posNum[Short]
    ipSubnet <- cidrV4
  } yield LocalNetwork(id, name, Some(VLan(vlan)), ipSubnet, None, None)

  val wan: Gen[WideAreaNetwork] = for {
    id <- networkId
    name <- networkName
    hiddenId <- Gen.oneOf("WAN", "WAN2")
    delete <- noDelete
  } yield WideAreaNetwork(id, name, Some(hiddenId), delete)

  val network: Gen[Network] = Gen.oneOf(defaultNetwork, lan, wan)

  val path: Gen[Path] = for {
    count <- Gen.choose(1, 5)
    sections <- Gen.listOfN(count, Gen.alphaNumStr.filter(!_.isEmpty))
  } yield FileSystems.getDefault.getPath(sections.head, sections.tail: _*)

  val firewallGroupId: Gen[FirewallGroupId] = for {
    id <- Gen.identifier
  } yield FirewallGroupId(id)

  val portGroup: Gen[PortGroup] = for {
    id <- firewallGroupId
    name <- Gen.identifier
    count <- Gen.choose(1, 5)
    members <- listOfN(count, Gen.choose(1, 65535))
    siteId <- siteId
  } yield PortGroup(id, name, members, siteId)

  val ipV4AddressSubnetGroup: Gen[Ipv4AddressSubnetGroup] = for {
    id <- firewallGroupId
    name <- Gen.identifier
    count <- Gen.choose(1, 5)
    members <- listOfN(count, Gen.oneOf(ipAddressV4, cidrV4))
    siteId <- siteId
  } yield Ipv4AddressSubnetGroup(id, name, members, siteId)

  val unknownFirewallGroup: Gen[UnknownFirewallGroup] = for {
    id <- firewallGroupId
    name <- Gen.identifier
    siteId <- siteId
  } yield UnknownFirewallGroup(id, name, siteId)

  val firewallGroup: Gen[FirewallGroup] = Gen.oneOf(portGroup, ipV4AddressSubnetGroup, unknownFirewallGroup)

  val firewallRuleId: Gen[FirewallRuleId] = for {
    id <- Gen.identifier
  } yield FirewallRuleId(id)

  val firewallRuleSourceAddressPortGroup: Gen[FirewallRule.SourceAddressPortGroup] = for {
    count <- Gen.choose(one - 1, 2)
    groups <- Gen.listOfN(count, firewallGroupId)
    macAddress <- Gen.option(Gen.identifier)
  } yield FirewallRule.SourceAddressPortGroup(groups, macAddress)

  val firewallRuleSourceNetwork: Gen[FirewallRule.SourceNetwork] = for {
    network <- networkId
    networkType <- Gen.oneOf(FirewallRule.IPv4Subnet, FirewallRule.GatewayIPAddress)
    macAddress <- Gen.option(Gen.identifier)
  } yield FirewallRule.SourceNetwork(network, networkType, macAddress)

  val firewallRuleSourceIPv4Address: Gen[FirewallRule.SourceIPv4Address] = for {
    ip <- ipAddressV4
    macAddress <- Gen.option(Gen.identifier)
  } yield FirewallRule.SourceIPv4Address(ip, macAddress)

  val firewallRuleSource: Gen[FirewallRule.Source] = Gen.oneOf(
    firewallRuleSourceAddressPortGroup,
    firewallRuleSourceNetwork,
    firewallRuleSourceIPv4Address
  )

  val firewallRuleDestinationAddressPortGroup: Gen[FirewallRule.DestinationAddressPortGroup] = for {
    count <- Gen.choose(one - 1, 2)
    groups <- Gen.listOfN(count, firewallGroupId)
  } yield FirewallRule.DestinationAddressPortGroup(groups)

  val firewallRuleDestinationNetwork: Gen[FirewallRule.DestinationNetwork] = for {
    network <-networkId
    networkType <- Gen.oneOf(FirewallRule.IPv4Subnet, FirewallRule.GatewayIPAddress)
  } yield FirewallRule.DestinationNetwork(network, networkType)

  val firewallRuleDestinationIPv4Address: Gen[FirewallRule.DestinationIPv4Address] = for {
    ip <- ipAddressV4
  } yield FirewallRule.DestinationIPv4Address(ip)

  val firewallRuleDestination: Gen[FirewallRule.Destination] = Gen.oneOf(
    firewallRuleDestinationAddressPortGroup,
    firewallRuleDestinationNetwork,
    firewallRuleDestinationIPv4Address
  )

  val firewallRuleProtocol: Gen[FirewallRule.Protocol] = Gen.oneOf(
    Gen.const(FirewallRule.AllProtocols),
    Gen.identifier.map(FirewallRule.SpecificProtocol),
    Gen.identifier.map(FirewallRule.AllExceptProtocol)
  )

  val firewallRule: Gen[FirewallRule] = for {
    id <- firewallRuleId
    siteId <- siteId
    name <- Gen.identifier
    index <- Gen.choose(2000, 2999)
    ruleSet <- Gen.oneOf(
      FirewallRule.WAN,
      FirewallRule.LAN,
      FirewallRule.Guest,
      FirewallRule.WANV6,
      FirewallRule.LANV6,
      FirewallRule.GuestV6
    )
    ruleSubset <- Gen.oneOf(FirewallRule.In, FirewallRule.Out, FirewallRule.Local)
    action <- Gen.oneOf(FirewallRule.Accept, FirewallRule.Drop, FirewallRule.Reject)
    source <- firewallRuleSource
    destination <- firewallRuleDestination
    enabled <- Gen.oneOf(true, false)
    advancedOptions <- Gen.containerOf[Set, FirewallRule.AdvancedOption](
      Gen.oneOf(
        FirewallRule.EnableLogging,
        FirewallRule.MatchStateNew,
        FirewallRule.MatchStateEstablished,
        FirewallRule.MatchStateInvalid,
        FirewallRule.MatchStateRelated
      )
    )
    protocol <- firewallRuleProtocol
    ipSecMatching <- Gen.oneOf(FirewallRule.DontMatchIpSec, FirewallRule.MatchInboundIpSec, FirewallRule.MatchInboundNonIpSec)
  }
    yield FirewallRule(
      id,
      siteId,
      name,
      index,
      ruleSet,
      ruleSubset,
      action,
      source,
      destination,
      enabled,
      advancedOptions,
      ipSecMatching,
      protocol
    )
}
