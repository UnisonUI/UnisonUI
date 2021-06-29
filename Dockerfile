FROM hexpm/elixir:1.12.1-erlang-24.0.1-alpine-3.13.3 as builder
WORKDIR /app
ENV MIX_ENV=prod

RUN apk add --no-cache git npm protoc && \
      mix local.rebar --force && \
      mix local.hex --force
      
COPY . /app/

ARG RELEASE=unisonui
RUN mix deps.get --only prod && \
      mix release ${RELEASE}

FROM alpine:3.13.3
ARG CONFD_VERSION=0.16.0
WORKDIR /app
ENV UNISON_UI_ROOT=/app

RUN apk add --no-cache git protoc bash
COPY docker/config.toml config.toml

ARG RELEASE=unisonui
ENV RELEASE=${RELEASE}

COPY --from=builder /app/_build/prod/rel/${RELEASE} .

ENTRYPOINT [ "/app/bin/${RELEASE}" ]
CMD [ "start" ]

