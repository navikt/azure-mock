FROM ghcr.io/navikt/sif-baseimages/java-25:2026.11.09.0747Z
LABEL org.opencontainers.image.source=https://github.com/navikt/azure-mock

COPY target/app.jar ./
