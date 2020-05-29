# RestUI

[![CircleCI](https://circleci.com/gh/MaethorNaur/restui.svg?style=svg)](https://circleci.com/gh/MaethorNaur/restui)
[![codecov](https://codecov.io/gh/MaethorNaur/restui/branch/master/graph/badge.svg)](https://codecov.io/gh/MaethorNaur/restui)
[![CodeFactor](https://www.codefactor.io/repository/github/maethornaur/restui/badge)](https://www.codefactor.io/repository/github/maethornaur/restui)

RestUI is a dynamic UI for **Swagger** REST definitions.

RestUI will discover automatically new services and expose the documentation
under one unified website.

Currently it can discover services through `Docker` and `Kubernetes` (inside the cluster)

## Overview

![overview](./docs/overview.png "Overview")

## Build

### Requirements

- JDK 11+
- NodeJS/Npm: Front the react application

### React application

There is two ways to build the React application:

- Using SBT
- Using only npm commands

#### Using SBT

```sh
sbt ";project rest-ui; npmInstall; webpackDevTask"
```

`webpackDevTask` can be replace by `webpackProdTask` if you want to produce minified assets.

#### Using NodeJS

```sh
cd rest-ui
npm install
npm run build
```

`npm run build` can be replace by `npm run prod` if you want to produce minified assets.

### RestUI

Once the react application build you are able to build RestUI

```sh
sbt "project rest-ui; packageBin"
```

This will produce a zip located: `rest-ui/target/universal/rest-ui-{VERSION}-SNAPSHOT.zip`

You also can use `docker:publishLocal` instead of `packageBin` if you want to directly produce a docker image

## Usage

This project is targeted for Java 11+ in order

### Configuration

RestUI uses an HOCON format for it's configuration.

Here is the default configuration used by RestUI.

In order to override the defaulr values you can either create your own configuration file
_(with only the fields you want to override)_, or pass the field through system properties:

It is also possible to combine the configuration file and system properties at the time, but
in that case, the system properties values will **prevail**.

The configuration file is passed as first parameter of RestUI:

```sh
rest-ui my-config.conf
```

If you want to override the value with system properties you have to do like this:
`-Drestui.http.port=3000`.

**Be careful** where an array is expected you have to override the value like so:
`-Drestui.providers.0=Provider1`, `-Drestui.providers.1=Provider2`, ...

```hocon
restui {
  // List of active providers
  // By default all available providers are activated
  // You can activate the the providers you want by overriding this field
  providers = [
    "restui.providers.git.GitProvider",
    "restui.providers.docker.DockerProvider",
    "restui.providers.kubernetes.KubernetesProvider"
  ]

  http {
    port = 8080 // Port of the Webserver
    interface = "0.0.0.0" // Interface where the webserver listen to
  }

  // Configuration for the docker provider
  // More information about how this provider works in the Docker provider section
  provider.docker {
    host =  "unix:///var/run/docker.sock" // Docker host

    // Labels name use to detect RestUI compatible container
    labels {
      port  = "restui.swagger.endpoint.port" // Label specifying the port on which the OpenApi spec is available.
      service-name = "restui.swagger.endpoint.service-name" // Label specifying the service name for RestUI.
      swagger-path = "restui.swagger.endpoint.swagger-path" // Label of the path where the OpenApi spec file is.
    }

  }

  // Configuration for the kubernetes provider
  // More information about how this provider works in the Kubernetes provider section
  provider.kubernetes {
    polling-interval = "1 minute" // Interval between each polling

    labels {
      port  = "restui.swagger.endpoint.port" // Label specifying the port on which the OpenApi spec is available.
      protocol = "restui.swagger.endpoint.protocol" // Label specifying which protocol the OpenApi spec is exposed.
      swagger-path = "restui.swagger.endpoint.swagger-path" // Label of the path where the OpenApi spec file is.
    }
  }

  // Configuration for the git provider
  // More information about how this provider works in the Git provider section
  provider.git {
    cache-duration = "2 hours" // Interval between each clone....
    vcs {
      // Specific to Github
      github {
        api-token = "" // Github personal token.
        api-uri = "https://api.github.com/graphql" // Github GraphQL url.
        polling-interval = "1 hour" // Interval between each polling.
        repositories = [] // List of repositories.
      }
      git {
        repositories = [] // List of repositories
      }
    }
  }

}
```

### Providers

#### Docker provider

The docker provider list all running containers and detect new and stopped containers.

In order to find compatible container, those containers **MUST** have the following labels on it:

- A label for the port where the OpenApi spec lays (default to: *restui.swagger.endpoint.port*)
- A label giving the service's name (default to: *restui.swagger.endpoint.service-name*)

The following labels are optional:

- A label specifying the path where the OpenApi spec lays (default to: *restui.swagger.endpoint.swagger-path*).
  If this label is not provided the default path is: **/swagger.yaml**

------------------------------------------------------------------------------------------------

Example:

```sh
docker  run --rm -l "restui.swagger.endpoint.port=80" -l "restui.swagger.endpoint.service-name=nginx" -v $(pwd):/usr/share/nginx/html:ro nginx:alpine
```

------------------------------------------------------------------------------------------------

#### Kubernetes provider

In order to make this provider work, RestUI **MUST** run inside the them Kubernetes
cluster as the services you want to discover.

The kubernetes provider list all running services and detect new services.

New services are detected by polling the kubernetes api at a regular interval.
The value for the interval is defined by `polling-interval` which default to `1 minute`.

In order to find compatible services, those services **MUST** have the following labels on it:

- A label for the port where the OpenApi spec lays (default to: *restui.swagger.endpoint.port*)
- A label specifying the protocol to use (default to: *restui.swagger.endpoint.protocol*)

The following labels are optional:

- A label specifying the path where the OpenApi spec lays (default to: *restui.swagger.endpoint.swagger-path*).
  If this label is not provided the default path is: **/swagger.yaml**

------------------------------------------------------------------------------------------------

Example:

```yaml
apiVersion: v1
kind: Service
metadata:
  labels:
    restui.swagger.endpoint.port: "80"
    restui.swagger.endpoint.protocol: http
  name: swagger
  namespace: default
spec:
  clusterIP: 10.96.0.2
  ports:
  - name: 80tcp02
    port: 80
    protocol: TCP
    targetPort: 80
  selector:
    selector: deployment
  sessionAffinity: None
  type: ClusterIP
status:
  loadBalancer: {}
---
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    selector: deployment
  name: swagger
  namespace: default
spec:
  progressDeadlineSeconds: 600
  replicas: 1
  revisionHistoryLimit: 10
  selector:
    matchLabels:
      selector: deployment
  strategy:
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
    type: RollingUpdate
  template:
    metadata:
      labels:
        selector: deployment
    spec:
      containers:
      - image: nginx:alpine
        imagePullPolicy: Always
        name: swagger5
        ports:
        - containerPort: 80
          name: 80tcp02
          protocol: TCP
        resources: {}
        securityContext:
          allowPrivilegeEscalation: false
          capabilities: {}
          privileged: false
          readOnlyRootFilesystem: false
          runAsNonRoot: false
        stdin: true
        terminationMessagePath: /dev/termination-log
        terminationMessagePolicy: File
        tty: true
      dnsPolicy: ClusterFirst
      restartPolicy: Always
      schedulerName: default-scheduler
      securityContext: {}
      terminationGracePeriodSeconds: 30
```

------------------------------------------------------------------------------------------------

#### Git provider

The git provider can be used to clone git repositories at regular interval (`cache-duration`).
You can either provider the list of repositories you to clone or use Github to discover repositories.

It's possible to use both options at the same time.

Each options requires a list of `repositories`. This list can be either a **string** corresponding to
the full (or `organization/project` for Github) or on object.

The object follows this schema:

```hocon
{
  location = "" // Full url, `organization/project` for Github or
                // a regex (the string **MUST** starts and ends with `/`)
  branch = "" // Branch to clone (default to `master` or inferred from the default branch in Github)
  swagger-paths = [] // List of OpenApi spec files or directories containing those kind of files
                     // inside your repository. Those paths are overrided by the restui configuration file inside
                     // of your repository.
}
```

If your repository contains a file at the root of it called `.restui.yaml`, the provider will
read it and get the OpenApi spec files or directories from it.

```yaml
# Service's name.
# If this field is not provide the service name will be inferred from the repository url
# Example: "https://github.com/MyOrg/MyRepo" -> "MyOrg/MyOrg"
name = "service name"
# List of OpenApi spec files or directories
swagger = []
```

If you intend to use the github option you need to provide a token.
This token can be generated [here](https://github.com/settings/tokens/new).
You will need at allow:

- `public_repo` if you want to list public repositories
- `repo`: if want to list public and private repositories

### Docker

```sh
docker pull maethornaur/rest-ui

docker run -p 8080:8080 maethornaur/rest-ui

```

Options can either be passed as an environment variable or as parameter.

To override the default value for a configuration entry (found in the
`reference.conf` files) just pass the `HOCON` path preceded by `-D`

```sh
docker run -p 8081:8081 maethornaur/rest-ui -Drestui.http.port=8081
```

There is a special case for configuration fields that are arrays.
You need to append the path by the index:
`restui.providers.0=restui.servicediscovery.docker.DockerProvider`

If you prefere you can use environment variable instead.
For that take the path, uppercase it and replace th `.` by `_` and `-` by `__`.

```sh
docker run -p 8081:8081 -e RESTUI_HTTP_PORT=8081 maethornaur/rest-ui
```
