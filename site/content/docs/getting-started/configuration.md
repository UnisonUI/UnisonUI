+++
title = "Configuration"
description = "How to configure UnisonUI"
date = 2020-12-24T04:53:02Z
weight = 10
draft = false
bref = ""
toc = true
+++

# Global configuration

UnisonUI uses `HOCON` configuration files.

`HOCON` is a superset of `JSON` and a mix of `JAVA` properties.

## Default configuration

```hocon
unisonui {
  // Provide UnisonUI specification inside UnisonUI
  self-specification = no

  // List of enabled providers (all by default)
  providers = [
    "tech.unisonui.providers.GitProvider",
    "tech.unisonui.providers.ContainerProvider",
    "tech.unisonui.providers.WebhookProvider"
  ]

  http {
    port = 8080 // Port of the Webserver
    interface = "0.0.0.0" // Interface where the webserver listen to
    statics-path="" // Path where static files (webapp) reside
  }

  // For more information about each provider configuration.
  // Please refered to it
  provider {
    docker {}
    kubernetes {}
    git {}
    webhook {}
  }
}
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
  -Dunisonui.providers.0=tech.unisonui.providers.git.GitProvider \
  -Dunisonui.providers.1=tech.unisonui.providers.git.WebhookProvider
```

-----
