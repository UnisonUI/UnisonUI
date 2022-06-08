---
sidebar_position: 4
title: "Logging"
description: "How to log"
---

## Default configuration

```toml
[logger.console]
level = 'warn'
format = 'logstash'
```

## Level

Available levels:

* `none` - disable logging
* `debug` or `all` - for debug-related messages 
* `info` - for information of any kind
* `warning` - for warnings
* `error` - for errors

## Format

If `format` is ommited, it fallbacks to: `$date $time [$level] $metadata$message\n`

### Logstash

Logs can be formatted in the `Logstash` if `format` is set to `logstash`.

Exemple:

```json
{
  "function": "init/1",
  "module": "Elixir.Horde.RegistryImpl",
  "otp_application": "horde",
  "@timestamp": "2022-06-08T19:10:16.964+00:00",
  "level": "info",
  "message": "Starting Horde.RegistryImpl with name Clustering.Registry"
}
```

### Text format

The format can be any string.


The following variables will be replace in the output:

* `$date` - Current UTC date
* `$level` - Log level
* `$message` - Actual message
* `$metadata` - Extra information like the `application`, `module` and `function`

Exemple:

```text
2022-06-08 19:17:03.690 [info] application=horde mfa=Horde.RegistryImpl.init/1 Starting Horde.RegistryImpl with name Clustering.Registry
```
