/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import type { CollectorsConfig } from '../types';

// The GET response before any config was saved: server-derived hostname, default port and thresholds.
export const unconfiguredCollectorsConfig: CollectorsConfig = {
  ca_cert_id: null,
  signing_cert_id: null,
  token_signing_key: null,
  otlp_server_cert_id: null,
  http: { hostname: 'graylog.example.com', port: 14401 },
  collector_heartbeat_interval: 'PT30S',
  collector_offline_threshold: 'PT5M',
  collector_default_visibility_threshold: 'P1D',
  collector_expiration_threshold: 'P7D',
};

// The same config after the first save bootstrapped the certificates and the token signing key.
export const configuredCollectorsConfig: CollectorsConfig = {
  ...unconfiguredCollectorsConfig,
  ca_cert_id: 'ca-id',
  signing_cert_id: 'signing-id',
  token_signing_key: {
    public_key: 'pub-key',
    private_key: 'priv-key',
    fingerprint: 'fp',
    created_at: '2026-01-01T00:00:00Z',
  },
  otlp_server_cert_id: 'otlp-id',
};

// A Collector Ingest (HTTP) input as returned by the inputs API, bound on the given port.
export const mockCollectorInput = (port: number = 14401) => ({
  id: 'input-1',
  creator_user_id: 'admin',
  node: 'node-1',
  name: 'CollectorIngestHttpInput',
  created_at: '2026-01-01T00:00:00Z',
  global: true,
  attributes: { port, bind_address: '0.0.0.0' },
  title: 'Collector Ingest (HTTP)',
  type: 'org.graylog.collectors.input.CollectorIngestHttpInput',
  content_pack: '',
  static_fields: {},
});
