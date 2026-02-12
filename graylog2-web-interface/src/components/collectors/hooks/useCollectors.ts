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
import { useQuery } from '@tanstack/react-query';

import type { SearchParams, Attribute } from 'stores/PaginationTypes';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import PaginationURL from 'util/PaginationURL';

import type { CollectorsConfig, Fleet, CollectorInstanceView, Source, CollectorStats } from '../types';
import { fetchCollectorsConfig } from './collectorsApi';
import {
  mockFleets,
  mockInstances,
  mockSources,
  mockStats,
  getFleetById,
  getInstancesByFleetId,
  getSourcesByFleetId,
  getFleetStats,
} from '../mockData';

const QUERY_KEY_PREFIX = 'collectors';

// Simulate API delay
const delay = (ms: number) => new Promise<void>((resolve) => {
  setTimeout(resolve, ms);
});

// Paginated response type matching Graylog standard
export type PaginatedCollectorsResponse<T> = {
  list: T[];
  pagination: { total: number };
  attributes: Attribute[];
};

// Key function for react-query
export const INSTANCES_KEY_PREFIX = ['collectors', 'instances', 'paginated'];
export const instancesKeyFn = (searchParams: SearchParams) => [...INSTANCES_KEY_PREFIX, searchParams];

const OFFLINE_THRESHOLD_MS = 5 * 60 * 1000;

const deriveStatus = (lastSeen: string): 'online' | 'offline' => {
  const elapsed = Date.now() - new Date(lastSeen).getTime();

  return elapsed < OFFLINE_THRESHOLD_MS ? 'online' : 'offline';
};

type ApiInstanceResponse = {
  instance_uid: string;
  fleet_id: string;
  capabilities: number;
  enrolled_at: string;
  last_seen: string;
  certificate_fingerprint: string;
  identifying_attributes: Record<string, unknown>;
  non_identifying_attributes: Record<string, unknown>;
};

const toView = (dto: ApiInstanceResponse): CollectorInstanceView => {
  const allAttributes = {...dto.identifying_attributes, ...dto.non_identifying_attributes};
  return ({
      id: dto.instance_uid,
      instance_uid: dto.instance_uid,
      fleet_id: dto.fleet_id,
      capabilities: dto.capabilities,
      enrolled_at: dto.enrolled_at,
      last_seen: dto.last_seen,
      certificate_fingerprint: dto.certificate_fingerprint,
      identifying_attributes: dto.identifying_attributes ?? {},
      non_identifying_attributes: dto.non_identifying_attributes ?? {},
      hostname: (allAttributes?.['host.name'] as string) ?? null,
      os: (allAttributes?.['os.type'] as string) ?? null,
      version: (allAttributes?.['service.version'] as string) ?? null,
      status: deriveStatus(dto.last_seen),
    });
};

const INSTANCE_ATTRIBUTES: Attribute[] = [
  { id: 'status', title: 'Status', type: 'STRING', sortable: false },
  { id: 'hostname', title: 'Hostname', type: 'STRING', sortable: false },
  { id: 'os', title: 'OS', type: 'STRING', sortable: false },
  { id: 'fleet_id', title: 'Fleet', type: 'STRING', sortable: false },
  { id: 'last_seen', title: 'Last Seen', type: 'DATE', sortable: false },
  { id: 'version', title: 'Version', type: 'STRING', sortable: false },
];

export const fetchPaginatedInstances = async (
  searchParams: SearchParams,
): Promise<PaginatedCollectorsResponse<CollectorInstanceView>> => {
  const url = PaginationURL('/collectors', searchParams.page, searchParams.pageSize);

  const response = await fetch('GET', qualifyUrl(url));

  return {
    list: (response.elements as ApiInstanceResponse[]).map(toView),
    pagination: response.pagination,
    attributes: INSTANCE_ATTRIBUTES,
  };
};

// Fleets paginated fetch
export const FLEETS_KEY_PREFIX = ['collectors', 'fleets', 'paginated'];
export const fleetsKeyFn = (searchParams: SearchParams) => [...FLEETS_KEY_PREFIX, searchParams];

export const fetchPaginatedFleets = async (
  searchParams: SearchParams,
): Promise<PaginatedCollectorsResponse<Fleet>> => {
  await delay(200);

  let filtered = [...mockFleets];

  // Apply search query
  if (searchParams.query) {
    const query = searchParams.query.toLowerCase();
    filtered = filtered.filter(
      (f) =>
        f.name.toLowerCase().includes(query) ||
        f.description.toLowerCase().includes(query),
    );
  }

  // Apply sorting
  if (searchParams.sort) {
    const { attributeId, direction } = searchParams.sort;
    filtered.sort((a, b) => {
      const aVal = a[attributeId as keyof Fleet] ?? '';
      const bVal = b[attributeId as keyof Fleet] ?? '';
      const cmp = String(aVal).localeCompare(String(bVal));

      return direction === 'asc' ? cmp : -cmp;
    });
  }

  // Apply pagination
  const total = filtered.length;
  const start = (searchParams.page - 1) * searchParams.pageSize;
  const paged = filtered.slice(start, start + searchParams.pageSize);

  return {
    list: paged,
    pagination: { total },
    attributes: [
      { id: 'name', title: 'Name', type: 'STRING', sortable: true },
      { id: 'description', title: 'Description', type: 'STRING', sortable: false },
      { id: 'target_version', title: 'Target Version', type: 'STRING', sortable: true },
      { id: 'created_at', title: 'Created', type: 'DATE', sortable: true },
    ],
  };
};

// Sources paginated fetch
export const SOURCES_KEY_PREFIX = ['collectors', 'sources', 'paginated'];
export const sourcesKeyFn = (searchParams: SearchParams) => [...SOURCES_KEY_PREFIX, searchParams];

export const fetchPaginatedSources = async (
  searchParams: SearchParams,
  fleetId?: string,
): Promise<PaginatedCollectorsResponse<Source>> => {
  await delay(200);

  let filtered = fleetId ? getSourcesByFleetId(fleetId) : [...mockSources];

  // Apply search query
  if (searchParams.query) {
    const query = searchParams.query.toLowerCase();
    filtered = filtered.filter(
      (s) =>
        s.name.toLowerCase().includes(query) ||
        s.description.toLowerCase().includes(query),
    );
  }

  // Apply filters
  if (searchParams.filters) {
    const typeFilter = searchParams.filters.get('type');
    if (typeFilter?.length) {
      filtered = filtered.filter((s) => typeFilter.includes(s.type));
    }
    const enabledFilter = searchParams.filters.get('enabled');
    if (enabledFilter?.length) {
      filtered = filtered.filter((s) => enabledFilter.includes(String(s.enabled)));
    }
  }

  // Apply sorting
  if (searchParams.sort) {
    const { attributeId, direction } = searchParams.sort;
    filtered.sort((a, b) => {
      const aVal = a[attributeId as keyof Source] ?? '';
      const bVal = b[attributeId as keyof Source] ?? '';
      const cmp = String(aVal).localeCompare(String(bVal));

      return direction === 'asc' ? cmp : -cmp;
    });
  }

  // Apply pagination
  const total = filtered.length;
  const start = (searchParams.page - 1) * searchParams.pageSize;
  const paged = filtered.slice(start, start + searchParams.pageSize);

  return {
    list: paged,
    pagination: { total },
    attributes: [
      { id: 'name', title: 'Name', type: 'STRING', sortable: true },
      { id: 'type', title: 'Type', type: 'STRING', sortable: true, filterable: true },
      { id: 'enabled', title: 'Enabled', type: 'BOOLEAN', sortable: true, filterable: true },
      { id: 'description', title: 'Description', type: 'STRING', sortable: false },
    ],
  };
};

export const useCollectorStats = () =>
  useQuery<CollectorStats>({
    queryKey: [QUERY_KEY_PREFIX, 'stats'],
    queryFn: async () => {
      await delay(200);

      return mockStats;
    },
  });

export const useFleets = () =>
  useQuery<Fleet[]>({
    queryKey: [QUERY_KEY_PREFIX, 'fleets'],
    queryFn: async () => {
      await delay(200);

      return mockFleets;
    },
  });

export const useFleet = (fleetId: string) =>
  useQuery<Fleet | undefined>({
    queryKey: [QUERY_KEY_PREFIX, 'fleets', fleetId],
    queryFn: async () => {
      await delay(200);

      return getFleetById(fleetId);
    },
    enabled: !!fleetId,
  });

export const useFleetStats = (fleetId: string) =>
  useQuery({
    queryKey: [QUERY_KEY_PREFIX, 'fleets', fleetId, 'stats'],
    queryFn: async () => {
      await delay(100);

      return getFleetStats(fleetId);
    },
    enabled: !!fleetId,
  });

export const useInstances = (fleetId?: string) =>
  useQuery<CollectorInstanceView[]>({
    queryKey: [QUERY_KEY_PREFIX, 'instances', { fleetId }],
    queryFn: async () => {
      await delay(200);

      return fleetId ? getInstancesByFleetId(fleetId) : mockInstances;
    },
  });

export const useSources = (fleetId?: string) =>
  useQuery<Source[]>({
    queryKey: [QUERY_KEY_PREFIX, 'sources', { fleetId }],
    queryFn: async () => {
      await delay(200);

      return fleetId ? getSourcesByFleetId(fleetId) : mockSources;
    },
  });

export const COLLECTORS_CONFIG_KEY = [QUERY_KEY_PREFIX, 'config'];

export const useCollectorsConfig = () =>
  useQuery<CollectorsConfig>({
    queryKey: COLLECTORS_CONFIG_KEY,
    queryFn: fetchCollectorsConfig,
  });
