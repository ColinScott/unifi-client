package com.abstractcode.unificlient.models

import com.abstractcode.unificlient.models.Identifiers._
import com.abstractcode.unificlient.models.Site.SiteName
import io.circe.{Decoder, Encoder, HCursor, Json}

case class Site(
  id: SiteId,
  name: SiteName,
  description: String,
  role: String,
  hiddenId: Option[String],
  noDelete: Option[Boolean]
)

object Site {
  case class SiteName(name: String)

  implicit val encodeSiteName: Encoder[SiteName] = (a: SiteName) => Json.fromString(a.name)
  implicit val decodeSiteName: Decoder[SiteName] = (c: HCursor) => c.as[String].map(SiteName)

  implicit val encodeSite: Encoder[Site] = Encoder.forProduct6[Site, SiteId, SiteName, String, String, Option[String], Option[Boolean]](
    "_id",
    "name",
    "desc",
    "role",
    "attr_hidden_id",
    "attr_no_delete"
  )(s => (s.id, s.name, s.description, s.role, s.hiddenId, s.noDelete))

  implicit val decodeSite: Decoder[Site] = Decoder.forProduct6[Site, SiteId, SiteName, String, String, Option[String], Option[Boolean]](
    "_id",
    "name",
    "desc",
    "role",
    "attr_hidden_id",
    "attr_no_delete"
  )(Site.apply)
}