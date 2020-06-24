import cats.data.Kleisli
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.abstractcode.unificlient.ControllerConfiguration.UniFiCredentials
import com.abstractcode.unificlient._
import com.abstractcode.unificlient.UniFiClient.UniFiRequest
import com.abstractcode.unificlient.models.Site
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._

import scala.concurrent.ExecutionContext.global

object Main extends IOApp {
  def listSites(uniFiClient: UniFiClient[IO]): UniFiRequest[IO, Unit] = for {
    sites <- uniFiClient.sites()
    _ <- Kleisli.liftF(IO(println(sites)))
    _ <- sites.traverse_(site => listSite(uniFiClient, site))
  } yield ()

  def listSite(uniFiClient: UniFiClient[IO], site: Site): UniFiRequest[IO, Unit] = for {
    networks <- uniFiClient.networks(site.name)
    firewallGroups <- uniFiClient.firewallGroups(site.name)
    firewallRules <- uniFiClient.firewallRules(site.name)
    _ <- Kleisli.liftF(IO(println(networks)))
    _ <- Kleisli.liftF(IO(println(firewallGroups)))
    _ <- Kleisli.liftF(IO(println(firewallRules)))
  } yield ()

  def run(args: List[String]): IO[ExitCode] = {
    val configuration = ControllerConfiguration(
      serverUri = uri"https://192.168.1.5:8443/",
      credentials = UniFiCredentials("readonly", "INCREDIBLY SECRET PASSWORD")
    )

    BlazeClientBuilder[IO](global)
      .resource.use {
      client =>
        val uniFiClient = new HttpUniFiClient[IO](client)

        val actions = for {
          _ <- listSites(uniFiClient)
          _ <- uniFiClient.logout()
        } yield ()

        actions.compose(uniFiClient.authenticate())
          .run(configuration)
          .map(_ => ExitCode.Success)
    }
  }
}