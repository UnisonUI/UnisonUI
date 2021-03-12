+++
title = "Container provider"
description = "How to use container provider"
date = 2021-01-18T18:25:09Z
weight = 30
draft = false
bref = ""
toc = true
+++

# Container provider

The container provider allow *UnisonUI* to discover services through `Docker` and `Kubernetes`

## Default configuration

```hocon
unisonui {
  providers += "tech.unisonui.providers.ContainerProvider"
  provider.container {
      kubernetes {
        enabled = yes // Enable discovery through Kubernetes
        polling-interval = "1 minute" // Interval between each Kubernetes API.
      }

      docker {
        enabled = yes // Enable discovery through Docker
        host =  "unix:///var/run/docker.sock" // Host of the docker daemon
      }

      // List of labels used by the provider to detect specification files
      labels {
        service-name = "unisonui.service-name" // Service name. This label is mandatory.

        openapi {
          port  = "unisonui.openapi.port" // HTTP port where the openapi specification file can be found.
          protocol  = "unisonui.openapi.protocol" // Protocol to use (default to http)
          specification-path = "unisonui.openapi.path" // URI of the openapi specification file. Default to /specification.yaml
          use-proxy = "unisonui.openapi.use-proxy" // Should enable the proxy for this service (disabled by default)
        }

        grpc {
          port = "unisonui.grpc.port" // GRPC port where the reflection server can be contacted.
          tls = "unisonui.grpc.tls" // Disabled by default. Tell the GRPC to use a TLS connection.
        }
      }
    }
}
```

## GRPC specification support

Both providers support GRPC specifications using.

In order to retrieve those specifications, your services need to expose the
[GRPC server reflection protocol](https://github.com/grpc/grpc/blob/master/doc/server-reflection.md).

## Docker services discovery

The docker services discovery list and detect all running containers in real time.

**Warning: the docker provider DOES NOT support TLS connection yet**

### Usage

A compatible container **MUST** include the following labels:

- A label specifying the service's name `unisonui.service-name`
- A label specifying the port where the OpenApi spec lays `unisonui.openapi.port` for OpenApi specifications.
- A label specifying the port where the GRPC spec lays `unisonui.grpc.port` for GRPC specifications.

Optional labels:

- A label specifying the path where the OpenApi spec lays `unisonui.openapi.path`.
  
  Default path: `/specification.yaml`

Example:

```sh
docker  run --rm -l "unisonui.port=80" -l "unisonui.service-name=nginx" -v $(pwd):/usr/share/nginx/html:ro nginx:alpine
```

## Kubernetes services discovery

The Kubernetes services discovery lists and detects all running services in real time.

In order to discover specifications in Kubernetes, *UnisonUI* **MUST** run inside the same Kubernetes cluster of your services you want to be discovered.

### Usage

New services are detected by polling from the Kubernetes API at a regular interval.
The value for the interval is defined by `polling-interval` which default to `1 minute`.

A compatible service **MUST** have the following labels on it:

- A label specifying the service's name `unisonui.service-name`
- A label specifying the port where the OpenApi spec lays `unisonui.openapi.port` for OpenApi specifications.
- A label specifying the port where the GRPC spec lays `unisonui.grpc.port` for GRPC specifications.

Optional labels:

- A label specifying the path where the OpenApi spec lays `unisonui.openapi.path`.
  
  Default path is: `/specification.yaml`

Also the services **MUST** have a `ClusterIP` (the provider will infer the address from the `ClusterIP`)

Example:

```yaml
apiVersion: v1
kind: Service
metadata:
  labels:
    unisonui.openapi.port: "80"
    unisonui.openapi.protocol: http
  name: specification
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
  name: openapi
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
        name: openapi
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
