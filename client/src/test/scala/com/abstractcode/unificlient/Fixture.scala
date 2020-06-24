package com.abstractcode.unificlient

import com.abstractcode.unificlient.UniFiClient.UniFiAccess
import com.abstractcode.unificlient.models.AuthCookies
import org.http4s.implicits._


object Fixture {
  val fixedUniFiAccess: UniFiAccess = new UniFiAccess(AuthCookies("a", "b"), uri"http://example.com/")
}
