package com.abstractcode.unificlient.models

import com.abstractcode.unificlient.Generators
import com.abstractcode.unificlient.models.FirewallGroup._
import io.circe.syntax._
import org.scalacheck.Prop.{forAll, propBoolean}
import org.scalacheck.Properties

object FirewallGroupSpec extends Properties("FirewallGroup") {
  property("can round trip encode and decode") = forAll(Generators.firewallGroup) {
    (firewallGroup: FirewallGroup) => firewallGroup.asJson.as[FirewallGroup] == Right(firewallGroup)
  }
}
