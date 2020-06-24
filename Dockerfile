FROM openjdk:11-jdk

ARG SBT_VERSION=1.3.10

RUN curl -L -o sbt-$SBT_VERSION.deb https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb
RUN dpkg -i sbt-$SBT_VERSION.deb
RUN rm sbt-$SBT_VERSION.deb
RUN apt-get update
RUN apt-get install sbt
RUN sbt sbtVersion

WORKDIR /app/

ADD . .

RUN sbt extractor/assembly

FROM openjdk:11-jre-slim

WORKDIR /app/
COPY --from=0 /app/extractor/target/scala-2.13/*.jar .

ENTRYPOINT java -jar unifi-markdown-extractor-assembly-*.jar