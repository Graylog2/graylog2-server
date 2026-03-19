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

// Domain types aligned with OpAMP backend schema

export type Fleet = {
  id: string;
  name: string;
  description?: string;
  target_version: string | null;
  created_at: string;
  updated_at: string;
};

export type CollectorInstanceView = {
  id: string;
  instance_uid: string;
  fleet_id: string;
  capabilities: number;
  enrolled_at: string;
  last_seen: string;
  certificate_fingerprint: string;
  identifying_attributes: Record<string, unknown>;
  non_identifying_attributes: Record<string, unknown>;
  hostname: string | null;
  os: string | null;
  version: string | null;
  status: 'online' | 'offline';
};

export type SourceType = 'file' | 'journald' | 'windows_event_log';

export type SourceBase = {
  id: string;
  fleet_id: string;
  name: string;
  description?: string;
  enabled: boolean;
  type: SourceType;
};

export type FileSourceConfig = {
  paths: string[];
  read_mode: 'beginning' | 'end';
  multiline?: { pattern: string; negate: boolean };
};

export type JournaldPriority = 'emerg' | 'alert' | 'crit' | 'err' | 'warning' | 'notice' | 'info' | 'debug';

export type JournaldSourceConfig = {
  priority: JournaldPriority;
  read_mode: 'beginning' | 'end';
  match_pattern?: string;
};

export type WindowsEventLogSourceConfig = {
  channels: string[];
  include_default_channels: boolean;
  read_mode: 'beginning' | 'end';
};

export type FileSource = SourceBase & { type: 'file'; config: FileSourceConfig };
export type JournaldSource = SourceBase & { type: 'journald'; config: JournaldSourceConfig };
export type WindowsEventLogSource = SourceBase & { type: 'windows_event_log'; config: WindowsEventLogSourceConfig };
export type Source = FileSource | JournaldSource | WindowsEventLogSource;

export type EnrollmentTokenCreator = {
  user_id: string;
  username: string;
};

export type EnrollmentTokenMetadata = {
  id: string;
  jti: string;
  kid: string;
  fleet_id: string;
  created_by: EnrollmentTokenCreator;
  created_at: string;
  expires_at: string | null;
  usage_count: number;
  last_used_at: string | null;
};

export type CollectorStats = {
  total_instances: number;
  online_instances: number;
  offline_instances: number;
  total_fleets: number;
  total_sources: number;
};

export type IngestEndpointConfig = {
  enabled: boolean;
  hostname: string;
  port: number;
  input_id: string | null;
};

export type CollectorsConfig = {
  ca_cert_id: string | null;
  signing_cert_id: string | null;
  token_signing_key: { is_set: boolean } | null;
  otlp_server_cert_id: string | null;
  http: IngestEndpointConfig;
  collector_offline_threshold: string;
  collector_default_visibility_threshold: string;
  collector_expiration_threshold: string;
};

export type CollectorsConfigRequest = {
  http: {
    enabled: boolean;
    hostname: string;
    port: number;
  };
  collector_offline_threshold: string;
  collector_default_visibility_threshold: string;
  collector_expiration_threshold: string;
};

export type FleetStatsSummary = {
  fleet_id: string;
  fleet_name: string;
  total_instances: number;
  online_instances: number;
  offline_instances: number;
  total_sources: number;
};

export type BulkFleetStatsResponse = {
  fleets: FleetStatsSummary[];
};

export type ActorInfo = {
  username: string;
  full_name: string;
};

export type TargetInfo = {
  id: string;
  name: string;
  type: 'fleet' | 'collector';
};

export type ActivityEntry = {
  seq: number;
  timestamp: string | null;
  type: 'CONFIG_CHANGED' | 'INGEST_CONFIG_CHANGED' | 'RESTART' | 'DISCOVERY_RUN' | 'FLEET_REASSIGNED';
  actor: ActorInfo | null;
  targets: TargetInfo[];
  details: Record<string, string>;
};

export type RecentActivityResponse = {
  activities: ActivityEntry[];
};
