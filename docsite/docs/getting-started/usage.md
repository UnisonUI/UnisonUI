---
sidebar_position: 1
title: "Usage"
description: "Up and running in under a minute"
---

# Getting started with UnisonUI

## Running UnisonUI

### Docker

```shell
docker pull unisonui/unisonui
docker run -p 8080:8080 unisonui/unisonui
```

The configuration file can be mounted at `/app/config.toml`.
If no configuration file is provided, one will be generated using environment
variables.

Example:

-----

```shell
docker run -p 8081:8081 -e UNISONUI_HTTP_PORT=8081 unisonui/unisonui
```

-----

More information about configuring UnisonUI can be found [here](configuration.md).

### From source

-----

__WARNING__: UnisonUI requires *Elxir 13* and *Erlang 24* in order to run.

-----


```shell
git clone https://github.com/UnisonUI/unisonui
cd unisonui # Go to the extracted folded
mix deps.get --only prod
MIX_ENV=prod mix release unisonui
export UNISON_UI_ROOT=/path/where/config.toml/is/
_build/prod/rel/bin/unisonui
```
