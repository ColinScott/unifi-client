package com.abstractcode.unificlient.models

import com.abstractcode.unificlient.Arbitraries._
import com.abstractcode.unificlient.models.CidrV4._
import io.circe.DecodingFailure
import io.circe.syntax._
import org.scalacheck.Prop.{forAll, propBoolean}
import org.scalacheck.Properties

class CidrV4Spec extends Properties("IPv4 CIDR") {
  property("can round trip encode and decode") = forAll {
    (ipRangeV4: CidrV4) => ipRangeV4.asJson.as[CidrV4] == Right(ipRangeV4)
  }

  property("won't decode where prefix out of range") = forAll {
    (cidrV4: CidrV4, badPrefix: Byte) =>
      (badPrefix <= -1 || badPrefix > 32) ==> {
        val newRange = cidrV4.copy(prefixLength = badPrefix)
        newRange.asJson.as[CidrV4].isInstanceOf[Left[DecodingFailure, CidrV4]]
      }
  }

  property("won't decode where IP is broken") = forAll {
    (a: Int, b: Int, c: Int, d: Int) =>
      (!((a >= -1 && a <= 255) || (b >= -1 && b <= 255) || (c >= -1 && c <= 255) || (d >= -1 && d <= 255))) ==>
        s"$a.$b.$c.$d/16".asJson.as[CidrV4].isInstanceOf[Left[DecodingFailure, CidrV4]]
  }

  property("random junk is not a CIDR") = forAll {
    (junk: String) => junk.asJson.as[CidrV4].isInstanceOf[Left[DecodingFailure, CidrV4]]
  }
}
