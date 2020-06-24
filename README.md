# AbstractCode UniFi Client

A Scala client library for querying Ubiquiti UniFi controllers. Also utilities (technically utility) using the client library.

Not associated with Ubiquiti.

## Disclaimer

Do not use this client for anything that needs to work reliably. Use of the client and the related utilities is at your own risk. No responsibility is taken for any damage, data loss, excommunications, legal consequences, paper cuts or any other outcome be it positive, negative or neutral (or any combination thereof) that may occur as  result of the use of this software. It comes with no warranty of any kind.

Do not use on hardware you do not have authority to use it on.

## Components

* [client](client/README.md) The client library
* [examples](examples) Example client usage
* [extractor](extractor/README.md) Produces Markdown describing some elements of the configured sites on a UniFi controller.

## Controller Version

This has been tested using controller version 5.13.30 on a UniFi Dream Machine Pro. Ir should also work against the same version on a Cloud Key and Cloud Key Gen 2 but this is not actively tested.

## Server URI

The Server URI will always use the IP or hostname of the controller instance. For the UniFi Dream Machine this will be:

```
https://<ip address/hostname>/proxy/network/
```

If you are using the default configuration then the following should work:

```
https://192.168.1.1/proxy/network/
```

For a Cloud Key it will be:

```
https://<ip address/hostname>:8443/
```

## Ongoing Compatibility

The controller API does not seem to be officially documented anywhere. This client has been built using unofficial sources and web browser network tools. Ubiquiti could change their API at any time in a new release at which point the client could break. I am not committing to fix it if this happens.
