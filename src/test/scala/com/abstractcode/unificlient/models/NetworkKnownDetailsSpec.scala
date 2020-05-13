package com.abstractcode.unificlient.models

import com.abstractcode.unificlient.models.Network._
import io.circe.parser._
import org.specs2.Specification
import org.specs2.matcher.MatchResult
import org.specs2.specification.core.SpecStructure

import scala.io.Source

class NetworkKnownDetailsSpec extends Specification {
  def is: SpecStructure =
    s2"""
       Network
          should be decoded from default $decodeFromDefault
          should be decoded from VLAN $decodeFromVlan
          should be decoded from WAN $decodeFromWan
    """

  def decodeFromDefault: MatchResult[Any] = {
    val decoded = parse(Source.fromResource("network-default.json").mkString).flatMap(_.as[Network])
    val expected = Right(LocalNetwork(
      id = NetworkId("3a919fc9a3a049b388d4e871f68a36b7"),
      name = NetworkName("LAN"),
      vlan = None,
      ipSubnet = CidrV4(IpAddressV4(192.toByte, 168.toByte, 1.toByte, 1.toByte), 22),
      hiddenId = Some("LAN"),
      noDelete = Some(true)
    ))
    decoded shouldEqual expected
  }

  def decodeFromVlan: MatchResult[Any] = {
    val decoded = parse(Source.fromResource("network-vlan.json").mkString).flatMap(_.as[Network])
    val expected = Right(LocalNetwork(
      id = NetworkId("6c5990c940b44f96ac14879bfad490af"),
      name = NetworkName("IoT"),
      vlan = Some(VLan(20)),
      ipSubnet = CidrV4(IpAddressV4(192.toByte, 168.toByte, 20.toByte, 1.toByte), 23),
      hiddenId = None,
      noDelete = None
    ))
    decoded shouldEqual expected
  }

  def decodeFromWan: MatchResult[Any] = {
    val decoded = parse(Source.fromResource("network-wan.json").mkString).flatMap(_.as[Network])
    val expected = Right(WideAreaNetwork(
      id = NetworkId("6cf3cade088b45fc9902f60e09a5b7bf"),
      name = NetworkName("WAN"),
      hiddenId = Some("WAN"),
      noDelete = Some(true)
    ))
    decoded shouldEqual expected
  }
}
