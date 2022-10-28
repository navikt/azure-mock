FROM amazoncorretto:17-alpine3.16
LABEL org.opencontainers.image.source=https://github.com/navikt/azure-mock

COPY target/app.jar ./