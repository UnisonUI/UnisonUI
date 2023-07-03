---
sidebar_position: 1
title: "Logging"
description: "How to log"
---

## Configuration

----
Example:

```toml
[logger.console]
level = 'info'
format = 'logstash'
```

----

## Level

The logging level is set by the environment variable: `LOGGER_LEVEL`
Available levels:

* `none` - disable logging
* `debug` or `all` - for debug-related messages 
* `info` - for information of any kind
* `warning` - for warnings
* `error` - for errors

## Format

The logging format is set by the environment variable: `LOGGER_FORMAT`

If `format` is ommited, it fallbacks to: `$date $time [$level] $metadata$message\n`

### Logstash

Logs can be formatted in the `Logstash` if `format` is set to `logstash`.

----
Example:

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

----

### Text format

The format can be any string.

The following variables will be replace in the output:

* `$date` - Current UTC date
* `$level` - Log level
* `$message` - Actual message
* `$metadata` - Extra information like the `application`, `module` and `function`

----
Example:

```text
2022-06-08 19:17:03.690 [info] application=horde mfa=Horde.RegistryImpl.init/1 Starting Horde.RegistryImpl with name Clustering.Registry
```

----
