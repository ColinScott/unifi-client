package com.abstractcode.unificlient.models

import io.circe.{Decoder, Encoder, HCursor, Json}

case class SiteId(id: String)
case class NetworkId(id: String)
case class FirewallGroupId(id: String)
case class FirewallRuleId(id: String)

object Identifiers {
  implicit val encodeSiteId: Encoder[SiteId] = (a: SiteId) => Json.fromString(a.id)
  implicit val decodeSiteId: Decoder[SiteId] = (c: HCursor) => c.as[String].map(SiteId)
  implicit val encodeNetworkId: Encoder[NetworkId] = (a: NetworkId) => Json.fromString(a.id)
  implicit val decodeNetworkId: Decoder[NetworkId] = (c: HCursor) => c.as[String].map(NetworkId)
  implicit val encodeFirewallGroupId: Encoder[FirewallGroupId] = (a: FirewallGroupId) => Json.fromString(a.id)
  implicit val decodeFirewallGroupId: Decoder[FirewallGroupId] = (c: HCursor) => c.as[String].map(FirewallGroupId)
  implicit val encodeFirewallRuleId: Encoder[FirewallRuleId] = (a: FirewallRuleId) => Json.fromString(a.id)
  implicit val decodeFirewallRuleId: Decoder[FirewallRuleId] = (c: HCursor) => c.as[String].map(FirewallRuleId)
}