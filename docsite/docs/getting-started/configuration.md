---
sidebar_position: 2
title: "Configuration"
description: "How to configure UnisonUI"
---

UnisonUI uses `TOML` configuration files.

The configuration **MUST** be named `config.toml` and the location of that file is defined
be the environment variable: `UNISON_UI_ROOT`

-----

If your `config.toml` path is `/my/path/config.toml`.
`UNISON_UI_ROOT` has to be set to `/my/path`

-----

## Default configuration

```toml
# More information in the logging section
[logger.console]
level = 'info'

[unison_ui]
self_spefication = false # Set by UNISONUI_SPECIFICATION
port = 8080 # Set by UNISONUI_HTTP_PORT

# More information in the Clustering section
[clustering]
provider = false

# More information in the Persistance section
[services]
storage_backend = 'Services.Storage.Memory'

# More information in the webhook provider section
[webhook_provider]
enabled = false

# More information in the git provider section
[git_provider]
enabled = false

# More information in the container provider section
[container_provider]
enabled = false
```
