FROM ghcr.io/navikt/sif-baseimages/java-25:2025.12.03.1527Z
LABEL org.opencontainers.image.source=https://github.com/navikt/azure-mock

COPY target/app.jar ./
