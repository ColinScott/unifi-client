package com.abstractcode.unifimarkdownextractor

import cats.data.NonEmptyList
import com.abstractcode.unifimarkdownextractor.MarkdownTableConverter.Column
import org.specs2.Specification
import org.specs2.matcher.MatchResult
import org.specs2.specification.core.SpecStructure

class MarkdownTableConverterSpec extends Specification {
  case class DataItem(one: String, two: Int, three: Double)

  def is: SpecStructure =
    s2"""
         Markdown Table Converter
            will convert single column single data $singleColumnSingleData
            will convert multiple columns single data $multipleColumnsSingleData
            will convert multiple columns multiple data $multipleColumnsMultipleData
            will encode values $encodeValues
            will convert newline to <br> $convertNewlineToHtmlBreak
      """

  def singleColumnSingleData: MatchResult[Any] = {
    val data = NonEmptyList.one(DataItem("test", 42, 4.5))

    val columns = NonEmptyList.one(Column[DataItem]("First", _.one))

    val expected =
      """| First |
        @|---|
        @| test |""".stripMargin('@')

    MarkdownTableConverter.convert(columns)(data) shouldEqual expected
  }

  def multipleColumnsSingleData: MatchResult[Any] = {
    val data = NonEmptyList.one(DataItem("test", 42, 4.5))

    val columns = NonEmptyList.of(
      Column[DataItem]("First", _.one),
      Column[DataItem]("Second", i => s"${i.two} ${i.three}"),
    )

    val expected =
      """| First | Second |
        @|---|---|
        @| test | 42 4.5 |""".stripMargin('@')

    MarkdownTableConverter.convert(columns)(data) shouldEqual expected
  }

  def multipleColumnsMultipleData: MatchResult[Any] = {
    val data = NonEmptyList.of(
      DataItem("test", 42, 4.5),
      DataItem("also test", -5436, 100.5),
      DataItem("something else", 333, -33)
    )

    val columns = NonEmptyList.of(
      Column[DataItem]("First", _.one),
      Column[DataItem]("Second", i => s"${i.two} ${i.three}"),
    )

    val expected =
      """| First | Second |
        @|---|---|
        @| test | 42 4.5 |
        @| also test | -5436 100.5 |
        @| something else | 333 -33.0 |""".stripMargin('@')

    MarkdownTableConverter.convert(columns)(data) shouldEqual expected
  }

  def encodeValues: MatchResult[Any] = {
    val data = NonEmptyList.of(
      DataItem("test | test", 42, 4.5)
    )


    val columns = NonEmptyList.of(
      Column[DataItem]("First", _.one)
    )

    val expected =
      """| First |
        @|---|
        @| test \| test |""".stripMargin('@')

    MarkdownTableConverter.convert(columns)(data) shouldEqual expected
  }

  def convertNewlineToHtmlBreak: MatchResult[Any] = {
    val data = NonEmptyList.of(
      DataItem("test \n test", 42, 4.5)
    )


    val columns = NonEmptyList.of(
      Column[DataItem]("First", _.one)
    )

    val expected =
      """| First |
        @|---|
        @| test <br/> test |""".stripMargin('@')

    MarkdownTableConverter.convert(columns)(data) shouldEqual expected
  }
}
