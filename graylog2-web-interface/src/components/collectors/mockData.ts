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
    instance_uid: 'uid-prod-web-01',
    capabilities: 15,
    fleet_id: 'fleet-prod-linux',
    enrolled_at: '2026-01-15T08:00:00Z',
    last_seen: new Date(Date.now() - 10000).toISOString(),
    certificate_fingerprint: 'aa:bb:cc',
    identifying_attributes: { 'service.name': 'graylog-collector', 'host.name': 'prod-web-01' },
    non_identifying_attributes: { 'os.type': 'linux', 'os.description': 'Ubuntu 22.04' },
    hostname: 'prod-web-01',
    os: 'linux',
    version: '1.2.0',
    status: 'online',
  },
  {
    id: 'inst-2',
    instance_uid: 'uid-prod-web-02',
    capabilities: 15,
    fleet_id: 'fleet-prod-linux',
    enrolled_at: '2026-01-15T08:30:00Z',
    last_seen: new Date(Date.now() - 8000).toISOString(),
    certificate_fingerprint: 'dd:ee:ff',
    identifying_attributes: { 'service.name': 'graylog-collector', 'host.name': 'prod-web-02' },
    non_identifying_attributes: { 'os.type': 'linux', 'os.description': 'Ubuntu 22.04' },
    hostname: 'prod-web-02',
    os: 'linux',
    version: '1.2.0',
    status: 'online',
  },
  {
    id: 'inst-3',
    instance_uid: 'uid-dev-03',
    capabilities: 15,
    fleet_id: 'fleet-linux-dev',
    enrolled_at: '2026-01-18T10:00:00Z',
    last_seen: new Date(Date.now() - 7200000).toISOString(),
    certificate_fingerprint: '11:22:33',
    identifying_attributes: { 'service.name': 'graylog-collector', 'host.name': 'dev-server-03' },
    non_identifying_attributes: { 'os.type': 'linux', 'os.description': 'Debian 12' },
    hostname: 'dev-server-03',
    os: 'linux',
    version: '1.1.0',
    status: 'offline',
  },
  {
    id: 'inst-4',
    instance_uid: 'uid-dc-primary',
    capabilities: 15,
    fleet_id: 'fleet-windows-dc',
    enrolled_at: '2026-01-12T14:00:00Z',
    last_seen: new Date(Date.now() - 12000).toISOString(),
    certificate_fingerprint: '44:55:66',
    identifying_attributes: { 'service.name': 'graylog-collector', 'host.name': 'dc-primary' },
    non_identifying_attributes: { 'os.type': 'windows', 'os.description': 'Windows Server 2022' },
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
