package com.abstractcode.unificlient.models

import com.abstractcode.unificlient.models.Site._
import io.circe.parser._
import org.specs2.Specification
import org.specs2.matcher.MatchResult
import org.specs2.specification.core.SpecStructure

import scala.io.Source

class SiteKnownDetailsSpec extends Specification {
  def is: SpecStructure =
    s2"""
       Site
          should be decoded from known JSON with optional attributes $decodeFromKnownJsonWithOptional
          should be decoded from known JSON without optional attributes $decodeFromKnownJsonWithoutOptional
    """

  def decodeFromKnownJsonWithOptional: MatchResult[Any] = {
    val decoded = parse(Source.fromResource("site-with-optional-attributes.json").mkString).flatMap(_.as[Site])
    val expected = Right(Site(
      id = SiteId("12345678"),
      name = SiteName("default"),
      description = "Default",
      role = "admin",
      hiddenId = Some("default"),
      noDelete = Some(true)
    ))
    decoded shouldEqual expected
  }

  def decodeFromKnownJsonWithoutOptional: MatchResult[Any] = {
    val decoded = parse(Source.fromResource("site-without-optional-attributes.json").mkString).flatMap(_.as[Site])
    val expected = Right(Site(
      id = SiteId("9876521213"),
      name = SiteName("erhqpnmlh"),
      description = "Other",
      role = "user",
      hiddenId = None,
      noDelete = None
    ))
    decoded shouldEqual expected
  }
}
