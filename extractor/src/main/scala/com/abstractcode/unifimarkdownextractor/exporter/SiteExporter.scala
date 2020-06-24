package com.abstractcode.unifimarkdownextractor.exporter

import java.nio.file.Path

import cats.data.{Kleisli, NonEmptyList}
import cats.implicits._
import cats.{Applicative, MonadError}
import com.abstractcode.unificlient.UniFiClient
import com.abstractcode.unificlient.UniFiClient.UniFiRequest
import com.abstractcode.unificlient.models._
import com.abstractcode.unifimarkdownextractor.configuration.ExportConfiguration

trait SiteExporter[F[_]] {
  def export(site: Site): UniFiRequest[F, Unit]
}

object SiteExporter {
  def apply[F[_]](implicit se: SiteExporter[F]): SiteExporter[F] = se
}

class FileSiteExporter[F[_] : UniFiClient : FileActions](exportConfiguration: ExportConfiguration)(implicit monadError: MonadError[F, Throwable]) extends SiteExporter[F] {
  val ruleSets: List[FirewallRule.RuleSet] = List(
    FirewallRule.WAN,
    FirewallRule.LAN,
    FirewallRule.Guest,
    FirewallRule.WANV6,
    FirewallRule.LANV6,
    FirewallRule.GuestV6
  )

  def export(site: Site): UniFiRequest[F, Unit] = for {
    networks <- UniFiClient[F].networks(site.name)
    firewallGroups <- UniFiClient[F].firewallGroups(site.name)
    firewallRules <- UniFiClient[F].firewallRules(site.name)
    _ <- Kleisli.liftF(write(site, networks, firewallGroups, firewallRules))
  } yield ()

  def write(site: Site, networks: List[Network], firewallGroups: List[FirewallGroup], firewallRules: List[FirewallRule]): F[Unit] = {
    val localNetworks = networks
      .flatMap { case l: LocalNetwork => Some(l) case _ => None }
      .sortBy(_.vlan.map(_.id).getOrElse(1: Short))
    for {
      siteDirectory <- monadError.catchNonFatal(exportConfiguration.basePath.resolve(site.name.name))
      _ <- FileActions[F].createDirectory(siteDirectory)
      _ <- writeLocalNetworks(siteDirectory, localNetworks)
      _ <- writeFirewallGroups(siteDirectory, firewallGroups)
      rulesWriter = (r: FirewallRule.RuleSet) => writeFirewallRules(siteDirectory, firewallGroups, networks, firewallRules)(r)
      _ <- ruleSets.traverse_(rulesWriter)

    } yield ()
  }

  def writeLocalNetworks(siteDirectory: Path, localNetworks: List[LocalNetwork]): F[Unit] = NonEmptyList.fromList(localNetworks) match {
    case Some(ln) => FileActions[F].write(siteDirectory.resolve("networks.md"), MarkdownConversion.localNetworks(ln))
    case None => Applicative[F].pure(())
  }

  def writeFirewallGroups(siteDirectory: Path, firewallGroups: List[FirewallGroup]): F[Unit] = NonEmptyList.fromList(firewallGroups) match {
    case Some(ln) => FileActions[F].write(siteDirectory.resolve("firewall-groups.md"), MarkdownConversion.firewallGroups(ln))
    case None => Applicative[F].pure(())
  }

  def writeFirewallRules(siteDirectory: Path, firewallGroups: List[FirewallGroup], networks: List[Network], firewallRules: List[FirewallRule])
    (ruleSet: FirewallRule.RuleSet): F[Unit] =
    NonEmptyList.fromList(firewallRules.filter(_.ruleSet == ruleSet).sortBy(_.index)) match {
      case Some(ln) =>
        val fileName = s"firewall-rules-${ruleSet.show.toLowerCase.replace(' ', '-')}.md"
        FileActions[F].write(siteDirectory.resolve(fileName), MarkdownConversion.firewallRules(firewallGroups, networks)(ln))
      case None => Applicative[F].pure(())
    }
}
