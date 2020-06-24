# UniFi Markdown Extractor

Extracts configuration information about UniFi sites and converts it into Markdown for documentation purposes.

The tool generates a directory structure containing a Markdown files documenting the network and firewall configuration. 

This tool is built on the [AbstractCode UniFi Client](../client/README.md)

## Disclaimer

This tool comes with no warranty of any kind. It may completely destroy your network or otherwise impair its functionality. Use at your own risk.

Do not use the tool on UniFi network equipment you are not authorised to access.

## Running

For general use the tool is intended to run inside a Docker container. This means users don't need to have anything but a Docker environment available.

The tool does not empty the target directory when run. It will overwrite files where there is new content but old files that do not have a new version will not be removed.

### Credentials

Although you can use your UniFi controller administrative credentials it is recommended that you create a separate account with read only to all sites but no permissions to make modifications such as device adoption.

### Docker Compose

Create a `docker-compose.yml` file as given below. This will pull the markdown extractor from Docker Hub. When run it will output the markdown into a directory called `out`.

```yaml
version: "3.3"

services:
  extractor:
    image: abstractcode/unifi-markdown-extractor:latest
    environment:
      SERVER_URI:
      USERNAME:
      PASSWORD:
      BASE_PATH: /out/
    volumes:
    - ./out:/out
```

You may set the environment variables one of three ways:

1. In your shell using its standard commands for setting environment variables. You will likely wish to exclude the set password command from your shell history (typically by prefacing the command with a space in most shells).
2. Edit the `docker-compose.yml` file to include the values.
3. Create a `.env` file in the directory containing `docker-compose.yml` and setting the values as key pairs (e.g. `USERNAME=readonly`)

If you put your password into a file treat it as you would any other file containing sensitive unencrypted material. If you are using source control consider excluding the file so your password is not exposed.

Once you have the environment variables configured you can run the tool with `docker-compose up`. The tool will exit once it completes so using `-d` with this command is of limited value.

### sbt

The tool is currently built using Java 11 and has not been tested with other versions. Assuming you have a Scala environment with a configured JDK available you can set the environment variables and use `sbt run` to run the tool.

### Environment Variables

The tool requires four environment variables to be configured. 

* **SERVER_URL** The base URL of the UniFi controller
* **USERNAME** Username for authentication to the controller
* **PASSWORD** Password for authentication to the controller
* **BASE_PATH** Path into which the Markdown should be written

