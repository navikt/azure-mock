FROM ghcr.io/navikt/sif-baseimages/java-25:2026.03.04.0913Z
LABEL org.opencontainers.image.source=https://github.com/navikt/azure-mock

COPY target/app.jar ./
