#!/usr/bin/env bash
opts=""

while IFS='=' read -r name value ; do
  if [[ $name == 'RESTUI_'* ]]; then
    value=${!name}
    name=${name//__/-}
    name=${name//_/.}
    name=$(echo "$name" | sed 's/./\L&/g')
    opts="${opts} -D${name}=${value}"
  fi
done < <(env)

$(pwd)/bin/$1 $opts ${@:2}
