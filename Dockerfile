FROM ghcr.io/navikt/sif-baseimages/java-25:2026.02.11.1141Z
LABEL org.opencontainers.image.source=https://github.com/navikt/azure-mock

COPY target/app.jar ./
