FROM ghcr.io/navikt/sif-baseimages/java-25:2026.04.16.1137Z
LABEL org.opencontainers.image.source=https://github.com/navikt/azure-mock

COPY target/app.jar ./
