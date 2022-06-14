FROM alpine:3.16.0
ARG CONFD_VERSION=0.16.0
RUN mkdir /app && chown -R nobody: /app
WORKDIR /app
ENV UNISON_UI_ROOT=/app

RUN apk add --no-cache git protoc bash curl
RUN curl -L https://github.com/kelseyhightower/confd/releases/download/v${CONFD_VERSION}/confd-${CONFD_VERSION}-linux-amd64 -o /bin/confd && \
  chmod a+x /bin/confd

COPY docker/confd /etc/confd
COPY docker/entrypoint.sh entrypoint.sh

RUN chmod a+x entrypoint.sh

USER nobody

ARG RELEASE=unisonui
ENV RELEASE=${RELEASE}

COPY _build/prod/rel/${RELEASE} .

ENTRYPOINT [ "/app/entrypoint.sh" ]
