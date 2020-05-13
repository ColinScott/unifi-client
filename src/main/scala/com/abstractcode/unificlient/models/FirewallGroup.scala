package com.abstractcode.unificlient.models

import com.abstractcode.unificlient.models.Identifiers._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Json}

sealed trait FirewallGroup {
  val id: FirewallGroupId
  val name: String
}

object FirewallGroup {
  case class PortGroup(
    id: FirewallGroupId,
    name: String,
    members: List[Int],
    siteId: SiteId
  ) extends FirewallGroup

  case class Ipv4AddressSubnetGroup(
    id: FirewallGroupId,
    name: String,
    members: List[IPv4],
    siteId: SiteId
  ) extends FirewallGroup

  case class UnknownFirewallGroup(
    id: FirewallGroupId,
    name: String,
    siteId: SiteId
  ) extends FirewallGroup

  implicit val encodeFirewallGroup: Encoder[FirewallGroup] = {
    case PortGroup(id, name, members, siteId) => Json.obj(
      ("_id", id.asJson),
      ("name", name.asJson),
      ("group_type", "port-group".asJson),
      ("group_members", members.asJson),
      ("site_id", siteId.asJson)
    )
    case Ipv4AddressSubnetGroup(id, name, members, siteId) => Json.obj(
      ("_id", id.asJson),
      ("name", name.asJson),
      ("group_type", "address-group".asJson),
      ("group_members", members.asJson),
      ("site_id", siteId.asJson)
    )
    case UnknownFirewallGroup(id, name, siteId) => Json.obj(
      ("_id", id.asJson),
      ("name", name.asJson),
      ("group_type", "".asJson),
      ("site_id", siteId.asJson)
    )
  }
  implicit val decodeFirewallGroup: Decoder[FirewallGroup] = (c: HCursor) => for {
    id <- c.downField("_id").as[FirewallGroupId]
    name <- c.downField("name").as[String]
    siteId <- c.downField("site_id").as[SiteId]
    groupType <- c.downField("group_type").as[String]
    firewallGroup <- groupType match {
      case "port-group" => for {
          members <- c.downField("group_members").as[List[Int]]
        } yield PortGroup(id, name, members, siteId)
      case "address-group" => for {
        members <- c.downField("group_members").as[List[IPv4]]
      } yield Ipv4AddressSubnetGroup(id, name, members, siteId)
      case _ => Right(UnknownFirewallGroup(id, name, siteId))
    }
  } yield firewallGroup

}