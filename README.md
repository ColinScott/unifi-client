# UniFi Client

A client for communicating with Ubiquiti UniFi controllers.

Not associated with Ubiquiti.

## Supported Actions

### Global

- **authenticate** Obtain authentication tokens from the controller. Required for all other actions.
- **logout** Invalidate the authentication token to prevent further use.
- **sites** Get a list of sites on the controller visible to the authenticated user.

### Per Site

- **networks** Get the networks associated with the site. Returns a subset of the API data only.
- **firewallGroups** Get the Firewall Groups, sets of ports or addresses that can be reused across multiple rules.
- **firewallRules** Get the rules for the UniFi firewall.

## Usage

These instructions assume you are using Cats `IO`. The API implementation only requires that your effect supports `cats.effect.Sync`.

### Configuration

Configuration is supplied to the client via [ControllerConfiguration](src/main/scala/com/abstractcode/unificlient/ControllerConfiguration.scala).

```scala
val configuration = ControllerConfiguration(
  serverUri = uri"https://192.168.1.5:8443/",
  credentials = UniFiCredentials("readonly", "INCREDIBLY SECRET PASSWORD")
)
```

You should probably load these values from an external source rather than hard code them.

### http4s Client

The UniFi client uses [http4s'](https://http4s.org/) client for making HTTP connections. We can use `BlazeClientBuilder` to construct one. This provides the client as a [Cats Resource](https://typelevel.org/cats-effect/datatypes/resource.html) that will be cleaned up automatically when we're done with it.

```scala
import scala.concurrent.ExecutionContext.global

BlazeClientBuilder[IO](global)
  .resource.use { client =>
    // Your code goes here
  }

```

#### Skipping certificate checks

By default your controller will use a self signed certificate that browsers and the http4s client will consider insecure. If you don't want to go to the trouble of installing a certificate from a certificate authority you can configure the client to skip certificate checks.

**THIS IS DELIBERATELY DISABLING SECURITY CHECKS. BE SURE YOU'RE OK WITH THE RISKS.**

We need to create an `SSLContext` that just trusts everything (this is usually a bad idea).

```scala
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.{SSLContext, X509TrustManager}

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
```

We then need to configure the `BlazeClientBuilder` to use if and to disable checking of endpoint authentication (this is also usually not a sensible thing to do).

```scala
BlazeClientBuilder[IO](global)
  .withSslContext(trustingSslContext)
  .withCheckEndpointAuthentication(false)
  .resource.use { client =>
    // Your code goes here
  }
```

Congratulations, you have measurably decreased the security of your software.

### Create the Client and Authenticate

The concrete implementation of `UniFiClient` is `HttpUniFiClient`.

```scala
BlazeClientBuilder[IO](global)
  .resource.use { client =>
    val uniFiClient = new HttpUniFiClient[IO](client)

    // Call the API somehow
  }
```

### Calling the API

With one exception all the API client methods have a return type of `UniFiResponse`. This is a type alias defined as:

```scala
type UniFiRequest[F[_], R] = Kleisli[F, UniFiAccess, R]
```

Using `Kleisli` means that the details required to access the API do not need to be passed to every request. We can compose a set of actions to be performed against the client using a for comprehension:

```scala
val actions: UniFiRequest[IO, List[Site]] = for {
  sites <- listSites(uniFiClient)
  _ <- uniFiClient.logout()
} yield sites
```

This describes a set of operations that gets the list of sites from the UniFi controller then cancels your authentication token. Actually doing these actions requires a `UniFiAccess` instance be available but this code doesn't require knowledge of what that is.

`actions` is just a description of a computation. We need to do a few things to make it useful. To start with we are unlikely to have a `UniFiAccess` instance. We do have a `ControllerConfiguration` which contains the location and authentication details of the UniFi controller. The `UniFiClient.authenticate` function provides a mechanism for going from a `ControllerConfiguration` to a `UniFiAccess`. This returns a `Kleisli[F, ControllerConfiguration, UniFiAccess]`.

As the `Kleisli`s used by `authenticate` and `action` are different we can't directly use them in a for comprehension. There's no type that matches `UniFiClient.UniFiAccess with ControllerConfiguration` which is what would be needed to satisfy the type constraints. Instead we can compose the functions:

```scala
val actions = for {
  sites <- listSites(uniFiClient)
  _ <- uniFiClient.logout()
} yield sites

actions.compose(uniFiClient.authenticate())
```

This works because the input type of `UniFiRequest` is the output type of the `authenticate` `Kleisli`.

```scala
BlazeClientBuilder[IO](global)
  .resource.use { client =>
    val uniFiClient = new HttpUniFiClient[IO](client)

    val actions = for {
      sites <- listSites(uniFiClient)
      _ <- uniFiClient.logout()
    } yield sites
    
    actions.compose(uniFiClient.authenticate())
      .run(configuration)
  }
```

You can lift functions in `IO` into `Kleisli` using `Kleisli.liftF` which means you don't need your entire program to use the `Kleisli`.

 ```scala
 val actions: UniFiRequest[IO, List[Site]] = for {
   sites <- listSites(uniFiClient)
   _ <- Kleisli.liftF(IO(println(sites)))
 } yield sites
 ```

### Example

```scala
package com.abstractcode.unificlient

import cats.data.Kleisli
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.abstractcode.unificlient.ControllerConfiguration.UniFiCredentials
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
```

## Dependencies

Recommended libraries for programs using the UniFi client.

```scala
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,

  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "org.http4s" %% "http4s-client" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,

  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-effect" % catsEffectVersion,
```