package com.abstractcode.unifimarkdownextractor

import java.nio.file.{FileSystems, Path}

import com.abstractcode.unificlient.ControllerConfiguration.UniFiCredentials
import org.http4s.Uri
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.Gen._

object Generators {
  val one = 1 // Work around Scala 2.13.2 bug
  val nonEmptyOrWhitespaceString: Gen[String] = arbitrary[String].suchThat(!_.trim.isEmpty)
  val whitespaceString: Gen[String] = Gen.chooseNum(one - 1, 32).map(" " * _)

  val uri: Gen[Uri] = for {
    protocol <- Gen.frequency(List((5, "http://"), (10, "https://")).map(freqTuple): _*)
    uri <- Gen.identifier
    port <- Gen.chooseNum[Int](minT = 1, maxT = 65535)
  } yield Uri.unsafeFromString(s"$protocol$uri:$port")

  val credentials: Gen[UniFiCredentials] = for {
    username <- nonEmptyOrWhitespaceString
    password <- nonEmptyOrWhitespaceString
  } yield UniFiCredentials(username, password)

  val path: Gen[Path] = for {
    count <- Gen.choose(1, 5)
    sections <- Gen.listOfN(count, Gen.alphaNumStr.filter(!_.isEmpty))
  } yield FileSystems.getDefault.getPath(sections.head, sections.tail: _*)

}
