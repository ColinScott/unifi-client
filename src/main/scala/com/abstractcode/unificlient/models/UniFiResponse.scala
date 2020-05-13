package com.abstractcode.unificlient.models

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class UniFiResponse[T](data: T)

object UniFiResponse {
  implicit def decodeUniFiResponse[T](implicit s: Decoder[T]): Decoder[UniFiResponse[T]] = deriveDecoder
  implicit def encodeUniFiResponse[T](implicit s: Encoder[T]): Encoder[UniFiResponse[T]] = deriveEncoder

  implicit def decodeUniFiResponseList[T](implicit s: Decoder[T]): Decoder[UniFiResponse[List[T]]] = deriveDecoder
  implicit def encodeUniFiResponseList[T](implicit s: Encoder[T]): Encoder[UniFiResponse[List[T]]] = deriveEncoder
}