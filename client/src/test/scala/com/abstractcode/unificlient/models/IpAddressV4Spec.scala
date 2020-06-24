package com.abstractcode.unificlient.models

import com.abstractcode.unificlient.Arbitraries._
import com.abstractcode.unificlient.models.IpAddressV4._
import io.circe.DecodingFailure
import io.circe.syntax._
import org.scalacheck.Prop.{forAll, propBoolean}
import org.scalacheck.Properties

object IpAddressV4Spec extends Properties("IPv4 Address") {
  property("can round trip encode and decode") = forAll {
    (ipAddressV4: IpAddressV4) => ipAddressV4.asJson.as[IpAddressV4] == Right(ipAddressV4)
  }

  property("insufficient parts") = forAll {
    (a: Byte, b: Byte, c: Byte) => s"${a & 0xff}.${b & 0xff}.${c & 0xff}".asJson.as[IpAddressV4].isInstanceOf[Left[DecodingFailure, IpAddressV4]]
  }

  property("too many parts") = forAll {
    (a: Byte, b: Byte, c: Byte, d: Byte, e: Byte) => s"${a & 0xff}.${b & 0xff}.${c & 0xff}.${d & 0xff}.${e & 0xff}".asJson.as[IpAddressV4].isInstanceOf[Left[DecodingFailure, IpAddressV4]]
  }

  property("numbers out of range") = forAll {
    (a: Int, b: Int, c: Int, d: Int) =>
      (!((a >= -1 && a <= 255) || (b >= -1 && b <= 255) || (c >= -1 && c <= 255) || (d >= -1 && d <= 255))) ==>
        (s"$a.$b.$c.$d".asJson.as[IpAddressV4].isInstanceOf[Left[DecodingFailure, IpAddressV4]])
  }

  property("random junk is not an IP") = forAll {
    (junk: String) => junk.asJson.as[IpAddressV4].isInstanceOf[Left[DecodingFailure, IpAddressV4]]
  }
}
