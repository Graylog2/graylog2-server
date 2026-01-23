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
import type {
  Fleet,
  CollectorInstanceView,
  Source,
  CollectorStats,
} from './types';

export const mockFleets: Fleet[] = [
  {
    id: 'fleet-prod-linux',
    name: 'production',
    description: 'Production Linux servers',
    target_version: '1.2.0',
    created_at: '2026-01-01T00:00:00Z',
    updated_at: '2026-01-20T10:30:00Z',
  },
  {
    id: 'fleet-windows-dc',
    name: 'windows-dc',
    description: 'Domain controllers',
    target_version: '1.2.0',
    created_at: '2026-01-05T00:00:00Z',
    updated_at: '2026-01-18T14:00:00Z',
  },
  {
    id: 'fleet-linux-dev',
    name: 'linux-dev',
    description: 'Development environment',
    target_version: null,
    created_at: '2026-01-10T00:00:00Z',
    updated_at: '2026-01-22T09:00:00Z',
  },
];

export const mockInstances: CollectorInstanceView[] = [
  {
    id: 'inst-1',
    agent_id: 'agent-prod-web-01',
    instance_uid: 'uid-prod-web-01',
    agent_description: {
      identifying_attributes: [
        { key: 'service.name', value: 'graylog-collector' },
        { key: 'host.name', value: 'prod-web-01' },
      ],
      non_identifying_attributes: [
        { key: 'os.type', value: 'linux' },
        { key: 'os.description', value: 'Ubuntu 22.04' },
      ],
    },
    remote_config_status: {
      last_remote_config_hash: 'a3f2c1d',
      status: 'APPLIED',
      error_message: null,
    },
    health: { healthy: true, start_time_unix_nano: Date.now() * 1e6, last_error: null },
    capabilities: 15,
    fleet_id: 'fleet-prod-linux',
    first_seen: '2026-01-15T08:00:00Z',
    last_seen: new Date(Date.now() - 10000).toISOString(),
    connection_type: 'WEBSOCKET',
    hostname: 'prod-web-01',
    os: 'linux',
    version: '1.2.0',
    status: 'online',
  },
  {
    id: 'inst-2',
    agent_id: 'agent-prod-web-02',
    instance_uid: 'uid-prod-web-02',
    agent_description: {
      identifying_attributes: [
        { key: 'service.name', value: 'graylog-collector' },
        { key: 'host.name', value: 'prod-web-02' },
      ],
      non_identifying_attributes: [
        { key: 'os.type', value: 'linux' },
        { key: 'os.description', value: 'Ubuntu 22.04' },
      ],
    },
    remote_config_status: {
      last_remote_config_hash: 'a3f2c1d',
      status: 'APPLIED',
      error_message: null,
    },
    health: { healthy: true, start_time_unix_nano: Date.now() * 1e6, last_error: null },
    capabilities: 15,
    fleet_id: 'fleet-prod-linux',
    first_seen: '2026-01-15T08:30:00Z',
    last_seen: new Date(Date.now() - 8000).toISOString(),
    connection_type: 'WEBSOCKET',
    hostname: 'prod-web-02',
    os: 'linux',
    version: '1.2.0',
    status: 'online',
  },
  {
    id: 'inst-3',
    agent_id: 'agent-dev-03',
    instance_uid: 'uid-dev-03',
    agent_description: {
      identifying_attributes: [
        { key: 'service.name', value: 'graylog-collector' },
        { key: 'host.name', value: 'dev-server-03' },
      ],
      non_identifying_attributes: [
        { key: 'os.type', value: 'linux' },
        { key: 'os.description', value: 'Debian 12' },
      ],
    },
    remote_config_status: {
      last_remote_config_hash: 'b4e3d2f',
      status: 'FAILED',
      error_message: 'Config validation error',
    },
    health: { healthy: false, start_time_unix_nano: 0, last_error: 'Connection lost' },
    capabilities: 15,
    fleet_id: 'fleet-linux-dev',
    first_seen: '2026-01-18T10:00:00Z',
    last_seen: new Date(Date.now() - 7200000).toISOString(),
    connection_type: 'HTTP',
    hostname: 'dev-server-03',
    os: 'linux',
    version: '1.1.0',
    status: 'offline',
  },
  {
    id: 'inst-4',
    agent_id: 'agent-dc-primary',
    instance_uid: 'uid-dc-primary',
    agent_description: {
      identifying_attributes: [
        { key: 'service.name', value: 'graylog-collector' },
        { key: 'host.name', value: 'dc-primary' },
      ],
      non_identifying_attributes: [
        { key: 'os.type', value: 'windows' },
        { key: 'os.description', value: 'Windows Server 2022' },
      ],
    },
    remote_config_status: {
      last_remote_config_hash: 'c5f4e3g',
      status: 'APPLIED',
      error_message: null,
    },
    health: { healthy: true, start_time_unix_nano: Date.now() * 1e6, last_error: null },
    capabilities: 15,
    fleet_id: 'fleet-windows-dc',
    first_seen: '2026-01-12T14:00:00Z',
    last_seen: new Date(Date.now() - 12000).toISOString(),
    connection_type: 'WEBSOCKET',
    hostname: 'dc-primary',
    os: 'windows',
    version: '1.2.0',
    status: 'online',
  },
];

export const mockSources: Source[] = [
  {
    id: 'src-app-logs',
    fleet_id: 'fleet-prod-linux',
    name: 'app-logs',
    description: 'Application server logs',
    enabled: true,
    type: 'file',
    config: {
      paths: ['/var/log/app/*.log'],
      read_mode: 'end',
    },
  },
  {
    id: 'src-nginx',
    fleet_id: 'fleet-prod-linux',
    name: 'nginx',
    description: 'Nginx access and error logs',
    enabled: true,
    type: 'file',
    config: {
      paths: ['/var/log/nginx/access.log', '/var/log/nginx/error.log'],
      read_mode: 'end',
    },
  },
  {
    id: 'src-journal',
    fleet_id: 'fleet-prod-linux',
    name: 'system-journal',
    description: 'Systemd journal',
    enabled: true,
    type: 'journald',
    config: {
      units: [],
      priority: 6,
    },
  },
  {
    id: 'src-security',
    fleet_id: 'fleet-windows-dc',
    name: 'security-events',
    description: 'Windows Security Event Log',
    enabled: true,
    type: 'windows_event_log',
    config: {
      channels: ['Security'],
      read_mode: 'end',
      event_format: 'json',
    },
  },
  {
    id: 'src-syslog',
    fleet_id: 'fleet-prod-linux',
    name: 'syslog-tcp',
    description: 'Syslog TCP receiver',
    enabled: true,
    type: 'tcp',
    config: {
      bind_address: '0.0.0.0',
      port: 5514,
      framing: 'newline',
    },
  },
];

export const mockStats: CollectorStats = {
  total_instances: mockInstances.length,
  online_instances: mockInstances.filter((i) => i.status === 'online').length,
  offline_instances: mockInstances.filter((i) => i.status === 'offline').length,
  total_fleets: mockFleets.length,
  total_sources: mockSources.length,
};

// Helper functions
export const getFleetById = (id: string): Fleet | undefined =>
  mockFleets.find((f) => f.id === id);

export const getInstancesByFleetId = (fleetId: string): CollectorInstanceView[] =>
  mockInstances.filter((i) => i.fleet_id === fleetId);

export const getSourcesByFleetId = (fleetId: string): Source[] =>
  mockSources.filter((s) => s.fleet_id === fleetId);

export const getFleetStats = (fleetId: string) => {
  const instances = getInstancesByFleetId(fleetId);
  const sources = getSourcesByFleetId(fleetId);

  return {
    total_instances: instances.length,
    online_instances: instances.filter((i) => i.status === 'online').length,
    offline_instances: instances.filter((i) => i.status === 'offline').length,
    total_sources: sources.length,
  };
};
