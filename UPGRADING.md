Upgrading to Graylog 5.1.x
==========================

## New Functionality

## Breaking Changes

## Behaviour Changes

The `JSON path value from HTTP API` input will now only run on the leader node,
if the `Global` option has been selected in the input configuration.
Previously, the input was started on all nodes in the cluster.
