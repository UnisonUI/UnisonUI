FROM hexpm/elixir:1.12.0-erlang-24.0-alpine-3.13.3 as builder
WORKDIR /app
ENV MIX_ENV=prod

RUN mix local.rebar --force && mix local.hex --force

RUN apk add --no-cache git npm protoc
COPY . /app/

RUN mix deps.get --only prod
RUN mix release

FROM alpine:3.13.3
ARG CONFD_VERSION=0.16.0
WORKDIR /app
ENV UNISON_UI_ROOT=/app/config/
RUN apk add --no-cache git protoc bash curl
RUN curl -L https://github.com/kelseyhightower/confd/releases/download/v${CONFD_VERSION}/confd-${CONFD_VERSION}-linux-amd64 > /usr/local/bin/confd && \
      chmod +x /usr/local/bin/confd
COPY docker/entrypoint.sh entrypoint.sh
COPY docker/confd/ confd/
COPY --from=builder /app/_build/prod/rel/unison_ui .

ENTRYPOINT [ "/app/entrypoint.sh" ]
CMD [ "start" ]

