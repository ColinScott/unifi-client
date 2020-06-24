package com.abstractcode.unificlient.models

import com.abstractcode.unificlient.models.FirewallGroup.{Ipv4AddressSubnetGroup, PortGroup, UnknownFirewallGroup}
import io.circe.parser._
import org.specs2.Specification
import org.specs2.matcher.MatchResult
import org.specs2.specification.core.SpecStructure

import scala.io.Source

class FirewallGroupKnownDetailsSpec extends Specification {
  def is: SpecStructure =
    s2"""
       Firewall Group
          should be decoded from port group $decodeFromPortGroup
          should be decoded from ipv4 address subnet group $decodeFromIpv4AddressSubnetGroup
          should be decoded from unknown firewall group $decodeFromUnknownFirewallGroup
    """

  def decodeFromPortGroup: MatchResult[Any] = {
    val decoded = parse(Source.fromResource("port-group.json").mkString).flatMap(_.as[FirewallGroup])
    val expected = Right(PortGroup(
      id = FirewallGroupId("891579ab266d466691ad761c6172390f"),
      name = "NTP Port",
      members = List(123, 456),
      siteId = SiteId("80ffec3ac97a4d5bac6770bac93b4ba0")
    ))
    decoded shouldEqual expected
  }

  def decodeFromIpv4AddressSubnetGroup: MatchResult[Any] = {
    val decoded = parse(Source.fromResource("ipv4-address-subnet-group.json").mkString).flatMap(_.as[FirewallGroup])
    val expected = Right(Ipv4AddressSubnetGroup(
      id = FirewallGroupId("68957123c1464c619ea91ad2d03a385f"),
      name = "Local Networks IPv4",
      members = List(
        IpAddressV4(10, 3, 2, 1),
        CidrV4(IpAddressV4(192.toByte, 168.toByte, 40, 1), 16)
      ),
      siteId = SiteId("4740757c1a8744b4b2c9e0ed420762c5")
    ))
    decoded shouldEqual expected
  }

  def decodeFromUnknownFirewallGroup: MatchResult[Any] = {
    val decoded = parse(Source.fromResource("unknown-firewall-group.json").mkString).flatMap(_.as[FirewallGroup])
    val expected = Right(UnknownFirewallGroup(
      id = FirewallGroupId("b769376bdb08471986001b8011f3c786"),
      name = "New type of group",
      siteId = SiteId("436c878ff1394c6ca4e40372bf6385ab")
    ))
    decoded shouldEqual expected
  }
}
