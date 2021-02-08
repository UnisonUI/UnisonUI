+++
title = "Getting started"
description = "Up and running in under a minute"
weight = -100
draft = false
toc = true
+++

# Getting started with UnisonUI

## Running UnisonUI

### Docker

```sh
docker pull unisonui/unisonui
docker run -p 8080:8080 unisonui/unisonui
```

More information about configuring UnisonUI can be found here.

In addition to the previous configuration method you can use variable
environments in order to configure it.

For that take the path, uppercase it and replace the `.` and `-`
respectively by `_` and `__`.

```sh
docker run -p 8081:8081 -e UNISONUI_HTTP_PORT=8081 unisonui/unisonui
```

### Binary

-----

__WARNING__: UnisonUI requires *Java 11+* in order to run.

-----

You can download the binary here.

Once downloaded unzip the package, go to the created folder and run:

```sh
curl https://github.com/UnisonUI/unisonui/releases/download/v1.0.0/unisonui.zip # Download the package
unzip unisonui.zip # Unzip the package
cd unisonui # Go to the extracted folded
bin/unisonui # Start UnisonUI
```

More information about how to configure UnisonUI can be found [here](docs/configuration/).

## Build from source
