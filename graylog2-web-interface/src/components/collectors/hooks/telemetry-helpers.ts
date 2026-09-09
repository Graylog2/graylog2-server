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
import type { CollectorInstanceView, Source } from '../types';

export type HostnameKind = 'ip' | 'hostname';
export type InputBindType = 'wildcard' | 'specific' | 'unknown';

const IPV4_PATTERN = /^(\d{1,3}\.){3}\d{1,3}$/;
const IPV6_PATTERN = /^[0-9a-fA-F:]+$/;

// Classifies a hostname string as an IP address or a DNS-style name.
// Empty string treated as 'hostname' (PostHog breakdown defaults are better than omitting).
export const classifyHostname = (value: string): HostnameKind => {
  if (!value) return 'hostname';
  if (IPV4_PATTERN.test(value)) return 'ip';
  if (value.includes(':') && IPV6_PATTERN.test(value)) return 'ip';

  return 'hostname';
};

// Classifies an Input's bind_address as wildcard (listens on any interface) or specific.
export const classifyInputBind = (bindAddress: string | undefined | null): InputBindType => {
  if (bindAddress === undefined || bindAddress === null) return 'unknown';
  if (bindAddress === '' || bindAddress === '0.0.0.0' || bindAddress === '::' || bindAddress === '*') {
    return 'wildcard';
  }

  return 'specific';
};

// Strips build metadata from a collector version, keeping the release identity.
// `0.3.1-SNAPSHOT+1caa145` -> `0.3.1-SNAPSHOT`. The build hash is debug detail and
// would give every build its own bucket in analytics breakdowns.
export const classifyVersion = (version: string | undefined | null): string | null => {
  if (!version) return null;

  return version.split('+')[0];
};

// Shared payload for telemetry events about a single collector instance, so the
// per-instance actions stay comparable to each other in analytics. Only carries what
// every caller holding a `CollectorInstanceView` already has -- callers that only know
// an instance UID intentionally send less rather than fetching extra data.
export const instanceTelemetryProps = (instance: CollectorInstanceView) => ({
  instance_id: instance.instance_uid,
  fleet_id: instance.fleet_id,
  status: instance.status,
  // Liveness (`status`) and config convergence are orthogonal: an instance can be
  // offline with changes still pending. Both are needed to reconstruct what the user saw.
  has_pending_changes: instance.has_pending_changes,
  version: classifyVersion(instance.version),
});

// Shared payload for telemetry events about a single source, matching the trio the
// create/update/delete events already send so all of them stay comparable.
export const sourceTelemetryProps = (source: Source, fleetId: string) => ({
  fleet_id: fleetId,
  source_id: source.id,
  source_type: source.type,
});
