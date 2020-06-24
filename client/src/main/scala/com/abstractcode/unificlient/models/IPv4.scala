package com.abstractcode.unificlient.models

import cats.Show
import cats.implicits._
import io.circe.syntax._
import io.circe._

sealed trait IPv4

case class IpAddressV4(a: Byte, b: Byte, c: Byte, d: Byte) extends IPv4

object IpAddressV4 {
  implicit val showIpAddressV4: Show[IpAddressV4] = Show.show(ip =>  s"${ip.a & 0xff}.${ip.b & 0xff}.${ip.c & 0xff}.${ip.d & 0xff}")

  implicit val encodeIpAddressV4: Encoder[IpAddressV4] = (a: IpAddressV4) => Json.fromString(a.show)
  implicit val decodeIpAddressV4: Decoder[IpAddressV4] = (c: HCursor) => c.as[String].flatMap(ip => {
    ip.split('.').toList.traverse(p => p.toShortOption.map(_.toByte)) match {
      case Some(a :: b :: c :: d :: Nil) =>
        Right(IpAddressV4(a, b, c, d))
      case _ => Left(DecodingFailure("Couldn't turn value into an IPv4 Address", Nil))
    }
  })
}

case class CidrV4(networkAddress: IpAddressV4, prefixLength: Byte) extends IPv4

object CidrV4 {
  implicit val showCidrV4: Show[CidrV4] = Show.show(cidr => s"${cidr.networkAddress.show}/${cidr.prefixLength}")

  implicit val cidrV4Encoder: Encoder[CidrV4] = (r: CidrV4) => Json.fromString(s"${r.networkAddress.show}/${r.prefixLength}")
  implicit val cidrV4Decoder: Decoder[CidrV4] = (c: HCursor) => c.as[String].flatMap(v => {
    v.split('/').toList match {
      case ipString :: prefixString :: Nil =>
        for {
          ip <- Json.fromString(ipString).as[IpAddressV4]
          prefix <- prefixString.toByteOption
            .filter(b => b > -1 && b <= 32)
            .map(_.asRight[DecodingFailure])
            .getOrElse(Left(DecodingFailure("Couldn't separate into the correct number of elements for an IPv4 CIDR", Nil)))
        } yield CidrV4(ip, prefix)
      case _ => Left(DecodingFailure("Couldn't separate into the correct number of elements for an IPv4 CIDR", Nil))
    }
  })
}

object IPv4 {
  implicit val showIPv4: Show[IPv4] = Show.show {
    case ip: IpAddressV4 => ip.show
    case cidr: CidrV4 => cidr.show
  }

  implicit val ipV4Encoder: Encoder[IPv4] = {
    case ip: IpAddressV4 => ip.asJson
    case cidr: CidrV4 => cidr.asJson
  }
  implicit val ipV4Decoder: Decoder[IPv4] = (c: HCursor) => {
    c.as[String].flatMap(i => if (i.contains('/')) c.as[CidrV4] else c.as[IpAddressV4])
  }
}