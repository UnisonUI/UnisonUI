---
sidebar_position: 2
title: "Clustering"
description: "How to configure UnisonUI"
toc_max_heading_level: 4
---

UnisonUI support clustering out of the box.
Clustering can be used to distribute the work load,
like pulling git repositories, handeling failures,

By default clustering is disabled.

Only one clustering mode can be active at a time.
The following sections are ordered by priority.

----

__WARNING__: When using clustering, do not use the `Memory` storage, as it can
leads to inconsistency. More information [here](storage.md)

----

The clustering system uses the port `9999`.

The format of a host is: `unisonui@HOSTNAME`.
If `HOSTNAME` is not set, it will be resolved using `hostname -f`

Regardless of the choosen mode, a *COOKIE* __MUST__ be defined.
That *COOKIE* __MUST__ have the same value across all instances.
The *COOKIE* value is set by the `RELEASE_COOKIE` environment variable.

## Hosts mode

In this mode, all instances of UnisonUI are defined in the configuration.

### Configuration

#### Environment variables

To active this mode using environment variables, you have to set at least
__ONE__ variable following the pattern: `CLUSTERING_HOSTS_*` where `*`
is an incremental number.

----

Example:

```shell
export CLUSTERING_HOSTS_0='unisonui@192.168.0.1'
export CLUSTERING_HOSTS_1='unisonui@192.168.0.2'
```

----

#### TOML

```toml
[clustering]
provider = 'hosts'
hosts = ['unisonui@192.168.0.1', 'unisonui@192.168.0.2']
```

----
