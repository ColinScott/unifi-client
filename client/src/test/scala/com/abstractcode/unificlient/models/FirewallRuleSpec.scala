package com.abstractcode.unificlient.models

import com.abstractcode.unificlient.Arbitraries._
import com.abstractcode.unificlient.models.FirewallRule._
import io.circe.syntax._
import org.scalacheck.Prop.{forAll, propBoolean}
import org.scalacheck.Properties

object FirewallRuleSpec extends Properties("FirewallRule") {
  property("can round trip encode and decode") = forAll {
    (firewallRule: FirewallRule) => firewallRule.asJson.as[FirewallRule] == Right(firewallRule)
  }
}
