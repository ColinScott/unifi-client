package com.abstractcode.unifimarkdownextractor

import cats.data.NonEmptyList

object MarkdownTableConverter {
  case class Column[T](title: String, extractor: T => String)

  def convert[T](columns: NonEmptyList[Column[T]])(data: NonEmptyList[T]): String = {
    def convertSingle(item: T): String = {
      columns.foldLeft("|")((b, column) => {
        val encodedItem = column.extractor(item)
          .replace("|", "\\|")
          .replace("\n", "<br/>")
        s"$b $encodedItem |"
      })
    }

    val header = columns.foldLeft("|")((b, column) =>s"$b ${column.title} |")
    val separator = columns.foldLeft("|")((b, _) =>s"$b---|")
    val content = data.map(convertSingle).toList.mkString("\n")

    s"$header\n$separator\n$content"
  }
}
