Upgrading to Graylog 7.0.0
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

## REST API Endpoint Changes

The following REST API changes have been made.

| Endpoint                                                              | Description                                                                             |
|-----------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| `GET /<endpoint>`                                                     | description                                                                             |
