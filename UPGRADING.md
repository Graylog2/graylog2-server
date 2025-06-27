Upgrading to Graylog 7.0.x
==========================

## Breaking Changes

### Kafka Inputs

The `kafka-clients` library was updated to 4.x which removes support for Kafka
brokers with version 2.0 and earlier. That means all Graylog 7.0 Kafka inputs
can only talk to Kafka brokers with version 2.1 or newer.

## Configuration File Changes

| Option        | Action     | Description                                    |
|---------------|------------|------------------------------------------------|
| `tbd`         | **added**  |                                                |

## Default Configuration Changes

- tbd

## Java API Changes

- tbd

## General REST API Changes

- In Graylog 7.0, an issue was fixed that previously allowed additional unknown JSON properties to be accepted 
  (and ignored) in API requests on the Graylog leader node. Now that the issue has been fixed, API requests on the 
  leader node will once again only accept JSON payloads that contain explicitly mapped/supported properties.  

## REST API Endpoint Changes

The following REST API changes have been made.

| Endpoint                                                              | Description                                                                             |
|-----------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| `GET /<endpoint>`                                                     | description                                                                             |
