---
sidebar_position: 2
title: "Configuration"
description: "How to configure UnisonUI"
---

UnisonUI uses `TOML` configuration files.

## Default configuration

```toml
[unison_ui]
self_spefication = true
port = 8080

[clustering]
provider = false
hosts = []

[clustering.kubernetes]
service = 'unisonui'

[clustering.aws]
tag_name = 'app'
tag_value = 'unisonui'

[services]
storage_backend = 'Services.Storage.Memory'

[services.raft]
quorum = 1
nodes = []
```

## Apply configuration

You can either pass your custom configuration as parameter
and/or uses java properties.

The order of priority for configuration value is:

```
Java property > custom configuration file > default value
```

### Using configuration file

```sh
./unisonui myconfig.conf
```

### Using java properties

```sh
./unisonui -Dunisonui.http.port=4242
```

-----
To set array value you have to follow this format: `property.{index}=value`

Example:

```sh
./unisonui \
  -Dunisonui.providers.0=tech.unisonui.providers.GitProvider \
  -Dunisonui.providers.1=tech.unisonui.providers.WebhookProvider
```

-----
