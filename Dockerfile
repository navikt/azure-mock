FROM ghcr.io/navikt/sif-baseimages/java-25:2026.05.04.0814Z
LABEL org.opencontainers.image.source=https://github.com/navikt/azure-mock

COPY target/app.jar ./
