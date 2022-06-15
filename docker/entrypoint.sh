#!/bin/bash
set -e
if [ ! -f /app/config.toml ]; then
  confd -onetime -backend env -log-level error
fi

/app/bin/${RELEASE} start
