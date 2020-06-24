package com.abstractcode.unifimarkdownextractor

import java.security.SecureRandom
import java.security.cert.X509Certificate

import cats.data.NonEmptyList
import cats.data.Validated.{Invalid, Valid}
import cats.effect._
import cats.implicits._
import com.abstractcode.unifimarkdownextractor.configuration.ParseError._
import com.abstractcode.unifimarkdownextractor.configuration.{AppConfiguration, ParseError}
import com.abstractcode.unifimarkdownextractor.exporter.{FileExporter, FileSiteExporter}
import com.abstractcode.unificlient.{HttpUniFiClient, UniFiClient}
import javax.net.ssl.{SSLContext, X509TrustManager}
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext.global

object Main extends IOApp {
  def process(appConfiguration: AppConfiguration, client: Client[IO]): IO[ExitCode] = {
    implicit val uniFiClient: UniFiClient[IO] = new HttpUniFiClient[IO](client)
    val exporter = new FileExporter[IO](appConfiguration.export, new FileSiteExporter[IO](appConfiguration.export))
    val action = for {
      _ <- exporter.export
      _ <- uniFiClient.logout()
    } yield ()

    action.compose(uniFiClient.authenticate())
      .run(appConfiguration.controller)
      .map(_ => ExitCode.Success)
  }

  def showConfigError(errors: NonEmptyList[ParseError]): IO[Unit] = IO(println(errors.map(_.show).toList.mkString("\n")))

  override def run(args: List[String]): IO[ExitCode] = {
    val trustingSslContext: SSLContext = {
      val trustManager = new X509TrustManager {
        def getAcceptedIssuers: Array[X509Certificate] = Array.empty

        override def checkClientTrusted(chain: Array[X509Certificate], authType: String): Unit = {}

        override def checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit = {}
      }
      val sslContext = SSLContext.getInstance("TLS")
      sslContext.init(null, Array(trustManager), new SecureRandom)
      sslContext
    }

    BlazeClientBuilder[IO](global)
      .withSslContext(trustingSslContext)
      .withCheckEndpointAuthentication(false)
      .resource.use { client =>
      for {
        env <- IO(sys.env)
        exitCode <- AppConfiguration(env) match {
          case Valid(config) => process(config, client)
          case Invalid(e) => showConfigError(e).map(_ => ExitCode.Error)
        }
      } yield exitCode
    }
  }
}
