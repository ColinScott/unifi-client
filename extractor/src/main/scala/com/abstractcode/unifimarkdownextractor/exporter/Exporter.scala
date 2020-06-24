package com.abstractcode.unifimarkdownextractor.exporter

import cats.data.{Kleisli, NonEmptyList}
import cats.effect._
import cats.implicits._
import com.abstractcode.unificlient
import com.abstractcode.unificlient.UniFiClient
import com.abstractcode.unificlient.UniFiClient.UniFiRequest
import com.abstractcode.unificlient.models.Site
import com.abstractcode.unifimarkdownextractor.configuration.ExportConfiguration

trait Exporter[F[_]] {
  def export: UniFiRequest[F, Unit]
}

class FileExporter[F[_] : Sync : UniFiClient](exportConfiguration: ExportConfiguration, siteExporter: SiteExporter[F]) extends Exporter[F] {
  def export: UniFiRequest[F, Unit] = for {
    sites <- implicitly[unificlient.UniFiClient[F]].sites()
    _ <- sites.traverse_(siteExporter.export)
    _ <- Kleisli.liftF(write(sites))
  } yield ()

  def write(sites: List[Site]): F[Unit] = for {
    _ <- FileActions.createDirectory[F](exportConfiguration.basePath)
    _ <- writeSites(sites)
  } yield ()

  def writeSites(localNetworks: List[Site]): F[Unit] = NonEmptyList.fromList(localNetworks) match {
    case Some(ln) => FileActions.write[F](exportConfiguration.basePath.resolve("sites.md"), MarkdownConversion.sites(ln))
    case None => Sync[F].pure(())
  }
}