#!/usr/bin/env bash
opts="-Dunisonui.http.statics-path=/app/statics/"

while IFS='=' read -r name value; do
  if [[ $name == 'UNISIONUI_'* ]]; then
    value=${!name}
    name=${name//__/-}
    name=${name//_/.}
    name=$(echo "$name" | sed 's/./\L&/g')
    opts="${opts} -D${name}=${value}"
  fi
done < <(env)

export JAVA_OPTS="${JAVA_OPTS} -Dlogback.configurationFile=/app/logback.xml"

/usr/local/bin/confd -onetime -backend env -prefix / -confdir /app/confd

$(pwd)/bin/$1 $opts ${@:2}
