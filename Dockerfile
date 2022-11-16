FROM ghcr.io/navikt/baseimages/temurin:17
LABEL org.opencontainers.image.source=https://github.com/navikt/azure-mock

COPY target/app.jar ./
