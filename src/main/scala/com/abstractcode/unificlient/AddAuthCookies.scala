package com.abstractcode.unificlient

import com.abstractcode.unificlient.models.AuthCookies
import org.http4s.Request

trait AddAuthCookies[R] {
  def addAuthCookies(request: R, authCookies: AuthCookies): R
}

object AddAuthCookies {
  def apply[R](implicit sh: AddAuthCookies[R]): AddAuthCookies[R] = sh

  implicit class AddAuthCookiesOps[R](val request: R) extends AnyVal {
    def addAuthCookies(authCookies: AuthCookies)(implicit a: AddAuthCookies[R]): R = a.addAuthCookies(request, authCookies)
  }

  implicit def ioRequestAddAuthCookies[F[_]]: AddAuthCookies[Request[F]] =
    (request, authCookies) => request.addCookie("unifises", authCookies.uniFiSes).addCookie("csrf_token", authCookies.csrfToken)
}
