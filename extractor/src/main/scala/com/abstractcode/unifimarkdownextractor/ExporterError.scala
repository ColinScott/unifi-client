package com.abstractcode.unifimarkdownextractor

import java.nio.file.Path

import com.abstractcode.unifimarkdownextractor.ExporterError._

sealed trait ExporterError extends Throwable {
  override def getMessage: String = this match {
    case DirectoryCreationFailure(path, true, false) => s"Tried to create directory $path but it exists and is not a directory."
    case DirectoryCreationFailure(path, exists, isDirectory) => s"Tried to create directory $path but failed (exists = $exists, isDirectory = $isDirectory)"
  }
}

object ExporterError {
  case class DirectoryCreationFailure(path: Path, exists: Boolean, isDirectory: Boolean) extends ExporterError
}