Upgrading to Graylog 5.1.x
==========================

## New Functionality

## Breaking Changes

## Behaviour Changes

- The default connection and read timeouts for email sending have been reduced from 60 seconds to 10 seconds.

## Configuration File Changes

| Option                                      | Action | Description                                                                                |
|---------------------------------------------|--------|--------------------------------------------------------------------------------------------|
| `transport_email_socket_connection_timeout` | added  | Connection timeout for establishing a connection to the email server. Default: 10 seconds. |
| `transport_email_socket_timeout`            | added  | Read timeout while communicating with the email server. Default: 10 seconds.               |
