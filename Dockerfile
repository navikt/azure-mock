FROM navikt/java:11
LABEL org.opencontainers.image.source=https://github.com/navikt/azure-mock

COPY target/app.jar ./