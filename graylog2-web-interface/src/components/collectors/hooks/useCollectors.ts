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

import type { Fleet, CollectorInstanceView, Source, CollectorStats } from '../types';
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

// Mock paginated fetch for instances
export const fetchPaginatedInstances = async (
  searchParams: SearchParams,
  fleetId?: string,
): Promise<PaginatedCollectorsResponse<CollectorInstanceView>> => {
  await delay(200);

  let filtered = fleetId ? getInstancesByFleetId(fleetId) : [...mockInstances];

  // Apply search query
  if (searchParams.query) {
    const query = searchParams.query.toLowerCase();
    filtered = filtered.filter(
      (i) =>
        i.hostname?.toLowerCase().includes(query) ||
        i.agent_id.toLowerCase().includes(query),
    );
  }

  // Apply filters
  if (searchParams.filters) {
    const statusFilter = searchParams.filters.get('status');
    if (statusFilter?.length) {
      filtered = filtered.filter((i) => statusFilter.includes(i.status));
    }
    const fleetFilter = searchParams.filters.get('fleet_id');
    if (fleetFilter?.length) {
      filtered = filtered.filter((i) => fleetFilter.includes(i.fleet_id));
    }
  }

  // Apply sorting
  if (searchParams.sort) {
    const { attributeId, direction } = searchParams.sort;
    filtered.sort((a, b) => {
      const aVal = a[attributeId as keyof CollectorInstanceView] ?? '';
      const bVal = b[attributeId as keyof CollectorInstanceView] ?? '';
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
      { id: 'status', title: 'Status', type: 'STRING', sortable: true, filterable: true },
      { id: 'hostname', title: 'Hostname', type: 'STRING', sortable: true },
      { id: 'os', title: 'OS', type: 'STRING', sortable: true },
      { id: 'fleet_id', title: 'Fleet', type: 'STRING', sortable: true, filterable: true },
      { id: 'last_seen', title: 'Last Seen', type: 'DATE', sortable: true },
      { id: 'version', title: 'Version', type: 'STRING', sortable: true },
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
