package com.abstractcode.unificlient.models

import cats.implicits._
import com.abstractcode.unificlient.Arbitraries._
import com.abstractcode.unificlient.models.Network._
import io.circe.syntax._
import org.scalacheck.Prop.{forAll, propBoolean}
import org.scalacheck.Properties

object NetworkSpec extends Properties("Network JSON") {
  property("can round trip encode and decode") = forAll {
    (network: Network) => network.asJson.as[Network] == Right(network)
  }

  property("show vlan") = forAll {
    (id: Short) => id.show == id.toString
  }

  property("show optional vlan") = forAll {
    (id: Option[Short]) => id.map(VLan).show == id.map(_.toString).getOrElse("1")
  }
}