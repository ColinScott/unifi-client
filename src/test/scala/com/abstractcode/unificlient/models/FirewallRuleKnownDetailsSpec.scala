package com.abstractcode.unificlient.models

import io.circe.parser._
import org.specs2.Specification
import org.specs2.matcher.MatchResult
import org.specs2.specification.core.SpecStructure

import scala.io.Source

class FirewallRuleKnownDetailsSpec extends Specification {
  def is: SpecStructure =
    s2"""
       Firewall Rule
          should be decoded from accept all established/related $decodeFromAcceptAllEstablishedRelated
          should be decoded from drop new/invalid for address/port $decodeFromDropNewInvalidForAddressPort
          should be decoded from reject for networks $decodeFromRejectForNetworks
          should be decoded from accept for IP Addresses $decodeFromAccessIpAddresses
    """

  def decodeFromAcceptAllEstablishedRelated: MatchResult[Any] = {
    val decoded = parse(Source.fromResource("firewall-rule-accept-all-established-related.json").mkString).flatMap(_.as[FirewallRule])
    val expected = Right(
      FirewallRule(
        FirewallRuleId("fba2ce30dda544dc83c0c9cc60aa5308"),
        SiteId("5055d7b30df14f4a80a9d279ba4b3d9d"),
        "All established/related",
        2000,
        FirewallRule.LAN,
        FirewallRule.In,
        FirewallRule.Accept,
        FirewallRule.SourceAddressPortGroup(Nil, None),
        FirewallRule.DestinationAddressPortGroup(Nil),
        enabled = true,
        Set(FirewallRule.MatchStateEstablished, FirewallRule.MatchStateRelated),
        FirewallRule.DontMatchIpSec,
        FirewallRule.AllProtocols
      )
    )
    decoded shouldEqual expected
  }

  def decodeFromDropNewInvalidForAddressPort: MatchResult[Any] = {
    val decoded = parse(Source.fromResource("firewall-rule-drop-new-invalid-for-address-port.json").mkString).flatMap(_.as[FirewallRule])
    val expected = Right(
      FirewallRule(
        FirewallRuleId("fba2ce30dda544dc83c0c9cc60aa5308"),
        SiteId("72b1617313034f478f40d83249de942c"),
        "Test 2",
        2002,
        FirewallRule.Guest,
        FirewallRule.Out,
        FirewallRule.Drop,
        FirewallRule.SourceAddressPortGroup(
          List(
            FirewallGroupId("38545d563d904f6585b0840c401f1bc0"),
            FirewallGroupId("c88cde38447244808d2336e57cf7d1aa")
          ),
          Some("00:00:00:00:00:00")),
        FirewallRule.DestinationAddressPortGroup(
          List(
            FirewallGroupId("1af44966b16142a993ad518790aba628"),
            FirewallGroupId("c448fe62c962480ba10bd8499817b73a")
          )
        ),
        enabled = false,
        Set(FirewallRule.EnableLogging, FirewallRule.MatchStateNew, FirewallRule.MatchStateInvalid),
        FirewallRule.MatchInboundIpSec,
        FirewallRule.AllExceptProtocol("udp")
      )
    )
    decoded shouldEqual expected
  }

  def decodeFromRejectForNetworks: MatchResult[Any] = {
    val decoded = parse(Source.fromResource("firewall-rule-reject-networks.json").mkString).flatMap(_.as[FirewallRule])
    val expected = Right(
      FirewallRule(
        FirewallRuleId("a6dcfff5a9b740e5aa0806cd8612cf52"),
        SiteId("566b2a3f45fa4b629e9cbbeb5170704d"),
        "Test 2",
        4000,
        FirewallRule.Guest,
        FirewallRule.Local,
        FirewallRule.Reject,
        FirewallRule.SourceNetwork(
          NetworkId("3de0a64dcf0f4bccaf3168be7d6c5381"),
          FirewallRule.GatewayIPAddress,
          Some("11:11:11:11:11:11")
        ),
        FirewallRule.DestinationNetwork(
          NetworkId("35da46875d0a409ebede41877519405e"),
          FirewallRule.IPv4Subnet
        ),
        enabled = true,
        Set(FirewallRule.EnableLogging, FirewallRule.MatchStateNew, FirewallRule.MatchStateInvalid),
        FirewallRule.MatchInboundNonIpSec,
        FirewallRule.SpecificProtocol("tcp")
      )
    )
    decoded shouldEqual expected
  }

  def decodeFromAccessIpAddresses: MatchResult[Any] = {
    val decoded = parse(Source.fromResource("firewall-rule-accept-ip-addresses.json").mkString).flatMap(_.as[FirewallRule])
    val expected = Right(
      FirewallRule(
        FirewallRuleId("005e164eb8744f388459680aad2946c5"),
        SiteId("4317f228d34c4448a920041942cf20b1"),
        "Test 3",
        2000,
        FirewallRule.Guest,
        FirewallRule.In,
        FirewallRule.Accept,
        FirewallRule.SourceIPv4Address(
          IpAddressV4(192.toByte, 168.toByte, 1, 1),
          Some("22:22:22:22:22:22")
        ),
        FirewallRule.DestinationIPv4Address(IpAddressV4(10, 2, 56, 1)),
        enabled = true,
        Set(),
        FirewallRule.DontMatchIpSec,
        FirewallRule.AllProtocols
      )
    )
    decoded shouldEqual expected
  }
}
