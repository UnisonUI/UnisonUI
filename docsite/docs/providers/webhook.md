---
sidebar_position: 3
title: "Webhook provider"
description: "How to use the webhook provider"
---

# Webhook provider

This provider allows to directly upload specifications to *UnisonUI*

## Default configuration

```html
[webhoo_provider]
enabled = true
self_spefication = true
port = 3000
```

## Webhook Api specification

Here is the OpanAPI specification for the webhook API

```yaml
openapi: 3.0.3
info:
  title: UnisonUI webhook
  description: >
    UnisonUI webhook is a service intends to give the ability to UnisonUI to add/remove services through webhook calls
  license:
    name: MIT
    url: https://raw.githubusercontent.com/UnisonUI/unisonui/master/LICENSE
  version: 1.0.0
paths:
  /services:
    post:
      summary: Create or update a service
      requestBody:
        description: Service to be inserted or update
        content:
          application/json:
            schema:
              oneOf:
                - $ref: '#/components/schemas/OpenApi' 
                - $ref: '#/components/schemas/Grpc' 
      responses:
        204:
          description: Service successfully inserted or updated
        500:
          description: Something bad happened
          content:
            text/plain:
              schema:
                type: string

  /services/{serviceName}:
    delete:
      summary: Delete a service
      parameters:
        - name: serviceName
          in: path
          description: Service's name to delete
          required: true
          schema:
            type: string
      responses:
        204:
          description: Service successfully deleted
        500:
          description: Something bad happened
          content:
            text/plain:
              schema:
                type: string
components:
  schemas:
    OpenApi:
      type: object
      required:
        - name
        - specification
      properties:
        name:
          type: string
          description: Service's name
        specification:
          type: string
          description: Specification file data
        metadata:
          type: object
          description: Optional metadata linked to the service
    Grpc:
      type: object
      required:
        - name
        - specification
      properties:
        name:
          type: string
          description: Service's name
        specification:
          type: string
          description: Specification file data
        servers:
          type: object
          additionalProperties:
            $ref: '#/components/schemas/Server'
        metadata:
          type: object
          description: Optional metadata linked to the service
    Server:
      type: object
      required:
        - address
        - port
        - useTls
      properties:
        address:
          type: string
        port:
          type: integer
        useTls:
          type: boolean
```
