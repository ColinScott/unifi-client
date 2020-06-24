package com.abstractcode.unifimarkdownextractor.exporter

import cats.Show
import cats.data.NonEmptyList
import cats.implicits._
import com.abstractcode.unificlient.models.IpAddressV4._
import com.abstractcode.unificlient.models._
import com.abstractcode.unifimarkdownextractor.MarkdownTableConverter
import com.abstractcode.unifimarkdownextractor.MarkdownTableConverter.Column

object MarkdownConversion {
  val sites: NonEmptyList[Site] => String = MarkdownTableConverter.convert(
    NonEmptyList.of[Column[Site]](
      Column("Name", _.name.name),
      Column("Description", _.description)
    )
  )

  val localNetworks: NonEmptyList[LocalNetwork] => String = MarkdownTableConverter.convert(
    NonEmptyList.of[Column[LocalNetwork]](
      Column("Network", _.name.name),
      Column("VLAN", _.vlan.show),
      Column("Network", _.ipSubnet.toString)
    )
  )

  val firewallGroups: NonEmptyList[FirewallGroup] => String = MarkdownTableConverter.convert(
    NonEmptyList.of[Column[FirewallGroup]](
      Column("Name", _.name),
      Column("Type", {
        case _: FirewallGroup.PortGroup => "Port Group"
        case _: FirewallGroup.Ipv4AddressSubnetGroup => "IPv4 Address/Subnet"
        case _ => "Unknown"
      }),
      Column("" , {
        case p: FirewallGroup.PortGroup => p.members.mkString(" ")
        case i: FirewallGroup.Ipv4AddressSubnetGroup => i.members.map(_.show).mkString("<br/>")
        case _ => ""
      })
    )
  )

  implicit val showFirewallGroup: Show[FirewallGroup] = Show.show {
    case pg: FirewallGroup.PortGroup => s"Ports:\n&nbsp;&nbsp;${pg.members.mkString("\n&nbsp;&nbsp;")}"
    case ip: FirewallGroup.Ipv4AddressSubnetGroup => s"IPv4:\n&nbsp;&nbsp;${ip.members.map(_.show).mkString("\n&nbsp;&nbsp;")}"
    case _: FirewallGroup.UnknownFirewallGroup => "Unknown group"
  }

  implicit val showNetwork: Show[Network] = Show.show {
    case lan: LocalNetwork => lan.name.name
    case wan: WideAreaNetwork => wan.name.name
  }

  implicit val showNetworkType: Show[FirewallRule.NetworkType] = Show.show {
    case FirewallRule.IPv4Subnet => "IPv4 Subnet"
    case FirewallRule.GatewayIPAddress => "Gateway IP Address"
  }

  implicit val showAdvancedOptions: Show[Set[FirewallRule.AdvancedOption]] = Show.show(
    options => {
      def hasOption(option: FirewallRule.AdvancedOption, message: String): Option[String] =
        if (options.contains(option)) Some(message) else None

      List(
        hasOption(FirewallRule.EnableLogging, "Logging enabled"),
        hasOption(FirewallRule.MatchStateNew, "Match New"),
        hasOption(FirewallRule.MatchStateEstablished, "Match Established"),
        hasOption(FirewallRule.MatchStateInvalid, "Match Invalid"),
        hasOption(FirewallRule.MatchStateRelated, "Match Related"),
      ).flatten.mkString("\n")
    }
  )

  def firewallRules(firewallGroups: List[FirewallGroup], networks: List[Network])(firewallRules: NonEmptyList[FirewallRule]): String = {
    val groups = firewallGroups.map(g => g.id -> g).toMap
    val networkMap = networks.map(n => n.id -> n).toMap

    def groupsDescription(fg: List[FirewallGroupId]): String =
      fg.traverse(g => groups.get(g).map(_.show)) match {
        case Some(Nil) => "Any"
        case Some(xs) => xs.mkString("\n")
        case None => "Unrecognised Firewall Groups"
      }

    def networkDescription(networkId: NetworkId, networkType: FirewallRule.NetworkType): String =
      networkMap.get(networkId)
        .map(n => s"Network ${n.show}\n(${networkType.show})")
        .getOrElse("Unrecognised Network")

    def addMac(description: String, macAddress: Option[String]): String =
      macAddress.map(mac => s"$description\nMac: $mac").getOrElse(description)

    MarkdownTableConverter.convert(
      NonEmptyList.of[Column[FirewallRule]](
        Column("Rule", _.index.toString),
        Column("Name", _.name),
        Column("Action", r => s"${r.action.show} ${r.protocol.show}"),
        Column("Enabled", _.enabled.toString),
        Column("Source", _.source match {
          case FirewallRule.SourceAddressPortGroup(fg, mac) =>
            addMac(groupsDescription(fg), mac)
          case FirewallRule.SourceNetwork(networkId, networkType, mac) =>
            addMac(networkDescription(networkId, networkType), mac)
          case FirewallRule.SourceIPv4Address(ipAddressV4, mac) =>
            addMac(ipAddressV4.show, mac)
        }),
        Column("Destination", _.destination match {
          case FirewallRule.DestinationAddressPortGroup(fg) => groupsDescription(fg)
          case FirewallRule.DestinationNetwork(networkId, networkType) => networkDescription(networkId, networkType)
          case FirewallRule.DestinationIPv4Address(ipAddressV4) => ipAddressV4.show
        }),
        Column("Advanced", _.advancedOptions.show)
      )
    )(firewallRules)
  }
}
