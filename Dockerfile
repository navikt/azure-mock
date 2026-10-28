FROM ghcr.io/navikt/sif-baseimages/java-25:2026.10.28.1413Z
LABEL org.opencontainers.image.source=https://github.com/navikt/azure-mock

COPY target/app.jar ./
