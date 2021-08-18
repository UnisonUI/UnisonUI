FROM alpine:3.14.0
RUN mkdir /app && chown -R nobody: /app
WORKDIR /app
ENV UNISON_UI_ROOT=/app

RUN apk add --no-cache git protoc bash
USER nobody

COPY docker/config.toml config.toml

ARG RELEASE=unisonui
ENV RELEASE=${RELEASE}

COPY _build/prod/rel/${RELEASE} .

ENTRYPOINT [ "/bin/bash", "-c", "/app/bin/${RELEASE} start" ]

