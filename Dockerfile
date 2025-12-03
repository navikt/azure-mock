FROM ghcr.io/navikt/sif-baseimages/java-25:2025.11.25.1015Z
LABEL org.opencontainers.image.source=https://github.com/navikt/azure-mock

COPY target/app.jar ./
