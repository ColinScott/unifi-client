package com.abstractcode.unificlient.models

import com.abstractcode.unificlient.Arbitraries._
import com.abstractcode.unificlient.models.Identifiers._
import io.circe.Json
import io.circe.syntax._
import org.scalacheck.Prop.forAll
import org.scalacheck.{Gen, Properties}

object IdentifiersSpec extends Properties("NetworkId JSON") {
  property("can round trip encode and decode") = forAll {
    (siteId: SiteId) => siteId.asJson.as[SiteId] == Right(siteId)
  }

  property("can decode from string") = forAll(Gen.identifier) {
    (id: String) => Json.fromString(id).as[SiteId] == Right(SiteId(id))
  }
  property("can round trip encode and decode") = forAll {
    (networkId: NetworkId) => networkId.asJson.as[NetworkId] == Right(networkId)
  }

  property("can decode from string") = forAll(Gen.identifier) {
    (id: String) => Json.fromString(id).as[NetworkId] == Right(NetworkId(id))
  }
}
