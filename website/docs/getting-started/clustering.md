---
sidebar_position: 3
title: "Clustering"
description: "How to configure UnisonUI"
---

## Default configuration

```toml
[logger.console]
level = 'warn'
format = 'logstash'
```

## Log level

Available levels:

* `none` - disable logging
* `debug` or `all` - for debug-related messages 
* `info` - for information of any kind
* `warning` - for warnings
* `error` - for errors

## Log format

`format` can be either:

* `logstash`
* `"$date $time [$level] $metadata$message\n"`. If format is not set it will
   fallback to this pattern
