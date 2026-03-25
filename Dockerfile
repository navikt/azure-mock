FROM ghcr.io/navikt/sif-baseimages/java-25:2026.03.25.1416Z
LABEL org.opencontainers.image.source=https://github.com/navikt/azure-mock

COPY target/app.jar ./
