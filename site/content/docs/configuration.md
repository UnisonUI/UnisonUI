+++
title = "Configuration"
description = "How to configure UnisonUI"
date = 2020-12-24T04:53:02Z
weight = 20
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
    "tech.unisonui.providers.git.GitProvider",
    "tech.unisonui.providers.docker.DockerProvider",
    "tech.unisonui.providers.kubernetes.KubernetesProvider",
    "tech.unisonui.providers.webhook.WebhookProvider"
  ]

  http {
    port = 8080 // Port of the Webserver
    interface = "0.0.0.0" // Interface where the webserver listen to
    statics-path="" // Path where static files (webapp) reside
  }
}
```
