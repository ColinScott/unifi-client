package com.abstractcode.unifimarkdownextractor.exporter

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.{Files, Path}

import cats._
import cats.effect._
import cats.implicits._
import com.abstractcode.unifimarkdownextractor.ExporterError.DirectoryCreationFailure

object FileActions {
  def createDirectory[F[_] : Sync](path: Path): F[Unit] = for {
    exists <- Sync[F].delay(Files.exists(path))
    isDirectory <- Sync[F].delay(Files.isDirectory(path))
    _ <- Applicative[F].pure(()).ensure(DirectoryCreationFailure(path, exists, isDirectory))(_ => !exists || isDirectory)
    _ <- if (exists) Applicative[F].pure(()) else Sync[F].delay(Files.createDirectory(path))
  } yield ()

  def write[F[_] : Sync](path: Path, content: String): F[Unit] = for {
    file <- Sync[F].delay(path.toFile)
    _ <- writer(file).use { w => Sync[F].delay(w.write(content)) }
  } yield ()

  def writer[F[_] : Sync](file: File): Resource[F, BufferedWriter] = Resource.fromAutoCloseable(
    Sync[F].delay(new BufferedWriter(new FileWriter(file)))
  )
}
