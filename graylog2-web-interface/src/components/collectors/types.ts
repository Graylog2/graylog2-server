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
  description: string;
  target_version: string | null;
  created_at: string;
  updated_at: string;
};

export type AgentDescription = {
  identifying_attributes: Array<{ key: string; value: string }>;
  non_identifying_attributes: Array<{ key: string; value: string }>;
};

export type RemoteConfigStatus = {
  last_remote_config_hash: string;
  status: 'UNSET' | 'APPLIED' | 'APPLYING' | 'FAILED';
  error_message: string | null;
};

export type AgentHealth = {
  healthy: boolean;
  start_time_unix_nano: number;
  last_error: string | null;
};

export type CollectorInstance = {
  id: string;
  agent_id: string;
  instance_uid: string;
  agent_description: AgentDescription;
  remote_config_status: RemoteConfigStatus;
  health: AgentHealth;
  capabilities: number;
  fleet_id: string;
  first_seen: string;
  last_seen: string;
  connection_type: 'HTTP' | 'WEBSOCKET';
};

export type CollectorInstanceView = CollectorInstance & {
  hostname: string | null;
  os: string | null;
  version: string | null;
  status: 'online' | 'offline';
};

export type SourceType = 'file' | 'journald' | 'windows_event_log' | 'tcp' | 'udp';

export type SourceBase = {
  id: string;
  fleet_id: string;
  name: string;
  description: string;
  enabled: boolean;
  type: SourceType;
};

export type FileSourceConfig = {
  paths: string[];
  read_mode: 'beginning' | 'end';
  multiline?: { pattern: string; negate: boolean };
};

export type JournaldSourceConfig = {
  units: string[];
  priority: number;
};

export type WindowsEventLogSourceConfig = {
  channels: string[];
  read_mode: 'beginning' | 'end';
  event_format: 'json' | 'xml';
};

export type TcpSourceConfig = {
  bind_address: string;
  port: number;
  framing: 'newline' | 'octet_counting';
};

export type UdpSourceConfig = {
  bind_address: string;
  port: number;
};

export type FileSource = SourceBase & { type: 'file'; config: FileSourceConfig };
export type JournaldSource = SourceBase & { type: 'journald'; config: JournaldSourceConfig };
export type WindowsEventLogSource = SourceBase & { type: 'windows_event_log'; config: WindowsEventLogSourceConfig };
export type TcpSource = SourceBase & { type: 'tcp'; config: TcpSourceConfig };
export type UdpSource = SourceBase & { type: 'udp'; config: UdpSourceConfig };

export type Source = FileSource | JournaldSource | WindowsEventLogSource | TcpSource | UdpSource;

export type EnrollmentToken = {
  id: string;
  fleet_id: string;
  expires_at: string;
  token: string;
};

export type CollectorStats = {
  total_instances: number;
  online_instances: number;
  offline_instances: number;
  total_fleets: number;
  total_sources: number;
};
