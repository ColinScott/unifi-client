package com.abstractcode.unificlient.models

import com.abstractcode.unificlient.models.FirewallRule._
import com.abstractcode.unificlient.models.Identifiers._
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}
import io.circe.syntax._
import cats.Show
import cats.implicits._

case class FirewallRule(
  id: FirewallRuleId,
  siteId: SiteId,
  name: String,
  index: Int,
  ruleSet: RuleSet,
  ruleSubset: RuleSubset,
  action: Action,
  source: Source,
  destination: Destination,
  enabled: Boolean,
  advancedOptions: Set[AdvancedOption],
  ipSecMatching: IpSecMatching,
  protocol: Protocol
)

object FirewallRule {
  sealed trait RuleSet

  case object WAN extends RuleSet
  case object LAN extends RuleSet
  case object Guest extends RuleSet
  case object WANV6 extends RuleSet
  case object LANV6 extends RuleSet
  case object GuestV6 extends RuleSet

  implicit val showFirewallRuleRuleSet: Show[RuleSet] = Show.show {
    case WAN => "WAN"
    case LAN => "LAN"
    case Guest => "Guest"
    case WANV6 => "WAN IPv6"
    case LANV6 => "LAN IPv6"
    case GuestV6 => "Guest IPv6"
  }

  implicit val ruleSetDecoder: Decoder[RuleSet] = (c: HCursor) =>
    c.as[String].map(_.split('_').toList).flatMap {
      case r :: _ :: Nil => r match {
        case "WAN" => Right(WAN)
        case "LAN" => Right(LAN)
        case "GUEST" => Right(Guest)
        case "WANv6" => Right(WANV6)
        case "LANv6" => Right(LANV6)
        case "GUESTv6" => Right(GuestV6)
        case _ => Left(DecodingFailure("Unknown ruleset", Nil))
      }
      case _ => Left(DecodingFailure("Ruleset format invalid", Nil))
    }

  sealed trait RuleSubset
  case object In extends RuleSubset
  case object Out extends RuleSubset
  case object Local extends RuleSubset

  implicit val ruleSubsetDecoder: Decoder[RuleSubset] = (c: HCursor) =>
    c.as[String].map(_.split('_').toList).flatMap {
      case _ :: s :: Nil => s match {
        case "IN" => Right(In)
        case "OUT" => Right(Out)
        case "LOCAL" => Right(Local)
        case _ => Left(DecodingFailure("Unknown ruleset direction", Nil))
      }
      case _ => Left(DecodingFailure("Ruleset format invalid", Nil))
    }

  implicit val ruleComponentsEncoder: Encoder[(RuleSet, RuleSubset)] = (a: (RuleSet, RuleSubset)) => {
    val major = a._1 match {
      case WAN => "WAN"
      case LAN => "LAN"
      case Guest => "GUEST"
      case WANV6 => "WANv6"
      case LANV6 => "LANv6"
      case GuestV6 => "GUESTv6"
    }
    val minor = a._2 match {
      case In => "IN"
      case Out => "OUT"
      case Local => "LOCAL"
    }

    s"${major}_$minor".asJson
  }

  sealed trait Action

  case object Accept extends Action
  case object Drop extends Action
  case object Reject extends Action

  implicit val showFirewallRuleAction: Show[Action] = Show.show {
    case Accept => "Accept"
    case Drop => "Drop"
    case Reject => "Reject"
  }

  sealed trait NetworkType

  case object IPv4Subnet extends NetworkType
  case object GatewayIPAddress extends NetworkType

  sealed trait HasFirewallGroups {
    val firewallGroups: List[FirewallGroupId]
  }
  sealed trait HasNetwork {
    val network: NetworkId
    val networkType: NetworkType
  }
  sealed trait HasIpAddressV4 {
    val ipAddressV4: IpAddressV4
  }

  sealed trait Source
  sealed trait HasMacAddress {
    val macAddress: Option[String]
  }

  case class SourceAddressPortGroup(firewallGroups: List[FirewallGroupId], macAddress: Option[String])
    extends Source with HasFirewallGroups with HasMacAddress
  case class SourceNetwork(
    network: NetworkId,
    networkType: NetworkType,
    macAddress: Option[String]
  ) extends Source with HasNetwork with HasMacAddress
  case class SourceIPv4Address(ipAddressV4: IpAddressV4, macAddress: Option[String])
    extends Source with HasIpAddressV4 with HasMacAddress

  sealed trait Destination

  case class DestinationAddressPortGroup(firewallGroups: List[FirewallGroupId])
    extends Destination with HasFirewallGroups
  case class DestinationNetwork(network: NetworkId, networkType: NetworkType)
    extends Destination with HasNetwork
  case class DestinationIPv4Address(ipAddressV4: IpAddressV4) extends Destination with HasIpAddressV4

  sealed trait AdvancedOption

  case object EnableLogging extends AdvancedOption
  case object MatchStateNew extends AdvancedOption
  case object MatchStateEstablished extends AdvancedOption
  case object MatchStateInvalid extends AdvancedOption
  case object MatchStateRelated extends AdvancedOption

  sealed trait IpSecMatching

  case object DontMatchIpSec extends IpSecMatching
  case object MatchInboundIpSec extends IpSecMatching
  case object MatchInboundNonIpSec extends IpSecMatching

  sealed trait Protocol
  case object AllProtocols extends Protocol
  case class SpecificProtocol(protocol: String) extends Protocol
  case class AllExceptProtocol(protocol: String) extends Protocol

  implicit val showFirewallRuleProtocol: Show[Protocol] = Show.show {
    case AllProtocols => "all"
    case SpecificProtocol(p) => p
    case AllExceptProtocol(p) => s"all except $p"
  }

  implicit val encodeFirewallRuleAction: Encoder[Action] = {
    case Accept => "accept".asJson
    case Drop => "drop".asJson
    case Reject => "reject".asJson
  }

  implicit val decodeFirewallRuleAction: Decoder[Action] = (c: HCursor) => c.as[String].flatMap {
    case "accept" => Right(Accept)
    case "drop" => Right(Drop)
    case "reject" => Right(Reject)
    case _ => Left(DecodingFailure("Unknown Firewall Rule Action", Nil))
  }

  implicit val encodeFirewallRuleIpSecMatching: Encoder[IpSecMatching] = {
    case DontMatchIpSec => "".asJson
    case MatchInboundIpSec => "match-ipsec".asJson
    case MatchInboundNonIpSec => "match-none".asJson
  }

  implicit val decodeFirewallRuleIpSecMatching: Decoder[IpSecMatching] = (c: HCursor) => c.as[String].flatMap {
    case "match-ipsec" => Right(MatchInboundIpSec)
    case "match-none" => Right(MatchInboundNonIpSec)
    case "" => Right(DontMatchIpSec)
    case _ => Left(DecodingFailure("Unknown IP Sec Action", Nil))
  }

  implicit val encodeFirewallRuleNetworkType: Encoder[NetworkType] = {
    case IPv4Subnet => "NETv4".asJson
    case GatewayIPAddress => "ADDRv4".asJson
  }

  implicit val decodeFirewallRuleNetworkType: Decoder[Option[NetworkType]] = (c: HCursor) => c.as[String].flatMap {
    case "NETv4" => Right(Some(IPv4Subnet))
    case "ADDRv4" => Right(Some(GatewayIPAddress))
    case "" => Right(None)
    case _ => Left(DecodingFailure("Unknown Network Type", Nil))
  }

  def getFirewallGroups[T, N <: T with HasFirewallGroups](t: T): Json = t match {
    case h: HasFirewallGroups => h.firewallGroups.asJson
    case _ => (Nil: List[Unit]).asJson
  }

  def getNetwork[T, N <: T with HasNetwork](t: T): Json = t match {
    case h: HasNetwork => h.network.asJson
    case _ => "".asJson
  }

  def getNetworkType[T, N <: T with HasNetwork](t: T): Json = t match {
    case h: HasNetwork => h.networkType.asJson
    case _ => "".asJson
  }

  def getIpAddressV4[T, N <: T with HasIpAddressV4](t: T): Json = t match {
    case h: HasIpAddressV4 => h.ipAddressV4.asJson
    case _ => "".asJson
  }

  def getMacAddress(source: Source): Json = (source match {
    case h: HasMacAddress => h.macAddress.getOrElse("")
    case _ => ""
  }).asJson

  def getProtocol(protocol: Protocol): Json = (protocol match {
    case SpecificProtocol(p) => p
    case AllExceptProtocol(p) => p
    case _ => "all"
  }).asJson

  implicit val encodeFirewallRule: Encoder[FirewallRule] = (r: FirewallRule) => Json.obj(
    ("_id", r.id.asJson),
    ("action", r.action.asJson),
    ("dst_firewallgroup_ids", getFirewallGroups[Destination, DestinationAddressPortGroup](r.destination)),
    ("enabled", r.enabled.asJson),
    ("ipsec", r.ipSecMatching.asJson),
    ("name", r.advancedOptions.contains(EnableLogging).asJson),
    ("name", r.name.asJson),
    ("ruleset", (r.ruleSet, r.ruleSubset).asJson),
    ("src_firewallgroup_ids", getFirewallGroups[Source, SourceAddressPortGroup](r.source)),
    ("src_mac_address", getMacAddress(r.source)),
    ("state_established", r.advancedOptions.contains(MatchStateEstablished).asJson),
    ("state_invalid", r.advancedOptions.contains(MatchStateInvalid).asJson),
    ("state_new", r.advancedOptions.contains(MatchStateNew).asJson),
    ("state_related", r.advancedOptions.contains(MatchStateRelated).asJson),
    ("dst_address", getIpAddressV4[Destination, DestinationIPv4Address](r.destination)),
    ("dst_networkconf_id", getNetwork[Destination, DestinationNetwork](r.destination)),
    ("dst_networkconf_type", getNetworkType[Destination, DestinationNetwork](r.destination)),
    ("src_address", getIpAddressV4[Source, SourceIPv4Address](r.source)),
    ("src_networkconf_id", getNetwork[Source, SourceNetwork](r.source)),
    ("src_networkconf_type",getNetworkType[Source, SourceNetwork](r.source)),
    ("rule_index", r.index.asJson),
    ("logging", r.advancedOptions.contains(EnableLogging).asJson),
    ("protocol_match_excepted", (r.protocol match { case _: AllExceptProtocol => true case _ => false }).asJson),
    ("protocol", getProtocol(r.protocol)),
    ("site_id", r.siteId.asJson)
  )

  implicit val decodeFirewallRuleSource: Decoder[Source] = (c: HCursor) => for {
    firewallGroupIds <- c.downField("src_firewallgroup_ids").as[List[FirewallGroupId]]
    network <- c.downField("src_networkconf_id").as[String].map(n => if (n.isEmpty) None else Some(NetworkId(n)))
    networkType <- c.downField("src_networkconf_type").as[Option[NetworkType]]
    address <- c.downField("src_address").as[IpAddressV4].map(a => Some(a)).recover {
      case _ => None
    }
    macAddress <- c.downField("src_mac_address").as[String].map(m => if (m.isEmpty) None else Some(m))
    source <- (firewallGroupIds, network, networkType, address) match {
      case (_, Some(n), Some(nt), _) => Right(SourceNetwork(n, nt, macAddress))
      case (_, _, _, Some(a)) => Right(SourceIPv4Address(a, macAddress))
      case (xs, _, _, _) => Right(SourceAddressPortGroup(xs, macAddress))
    }
  } yield source

  implicit val decodeFirewallRuleDestination: Decoder[Destination] = (c: HCursor) => for {
    firewallGroupIds <- c.downField("dst_firewallgroup_ids").as[List[FirewallGroupId]]
    network <- c.downField("dst_networkconf_id").as[String].map(n => if (n.isEmpty) None else Some(NetworkId(n)))
    networkType <- c.downField("dst_networkconf_type").as[Option[NetworkType]]
    address <- c.downField("dst_address").as[IpAddressV4].map(a => Some(a)).recover {
      case _ => None
    }
    destination <- (firewallGroupIds, network, networkType, address) match {
      case (_, Some(n), Some(nt), _) => Right(DestinationNetwork(n, nt))
      case (_, _, _, Some(a)) => Right(DestinationIPv4Address(a))
      case (xs, _, _, _) => Right(DestinationAddressPortGroup(xs))
    }
  } yield destination

  implicit val decodeFirewallRuleAdvancedOptions: Decoder[Set[AdvancedOption]] = (c:HCursor) => for {
    logging <- c.downField("logging").as[Boolean].map(toOption(_, EnableLogging))
    stateNew <- c.downField("state_new").as[Boolean].map(toOption(_, MatchStateNew))
    stateEstablished <- c.downField("state_established").as[Boolean].map(toOption(_, MatchStateEstablished))
    stateInvalid <- c.downField("state_invalid").as[Boolean].map(toOption(_, MatchStateInvalid))
    stateRelated <- c.downField("state_related").as[Boolean].map(toOption(_, MatchStateRelated))
  } yield (logging :: stateNew :: stateEstablished :: stateInvalid :: stateRelated :: Nil).flatten.toSet

  implicit val decodeFirewallRuleProtocol: Decoder[Protocol] = (c: HCursor) => for {
    protocol <- c.downField("protocol").as[String]
    excepted <- c.downField("protocol_match_excepted").as[Boolean]
  } yield (protocol, excepted) match {
    case ("all", _) => AllProtocols
    case (p, false) => SpecificProtocol(p)
    case (p, _) => AllExceptProtocol(p)
  }

  implicit val decodeFirewallRule: Decoder[FirewallRule] = (c: HCursor) => for {
    id <- c.downField("_id").as[FirewallRuleId]
    siteId <- c.downField("site_id").as[SiteId]
    name <- c.downField("name").as[String]
    index <- c.downField("rule_index").as[Int]
    rulesetParts = c.downField("ruleset")
    ruleset <- rulesetParts.as[RuleSet]
    ruleSubset <- rulesetParts.as[RuleSubset]
    action <- c.downField("action").as[Action]
    source <- c.as[Source]
    destination <- c.as[Destination]
    enabled <- c.downField("enabled").as[Boolean]
    advancedOptions <- c.as[Set[AdvancedOption]]
    ipSecMatching <- c.downField("ipsec").as[IpSecMatching]
    protocol <- c.as[Protocol]
  } yield FirewallRule(
    id,
    siteId,
    name,
    index,
    ruleset,
    ruleSubset,
    action,
    source,
    destination,
    enabled,
    advancedOptions,
    ipSecMatching,
    protocol
  )

  def toOption(flag: Boolean, option: AdvancedOption): Option[AdvancedOption] =
    if (flag) Some(option) else None
}
