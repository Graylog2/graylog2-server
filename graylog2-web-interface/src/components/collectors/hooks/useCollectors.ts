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

import { SystemInputs } from '@graylog/server-api';
import type { SearchParams, Attribute } from 'stores/PaginationTypes';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import PaginationURL from 'util/PaginationURL';
import type { InputSummary } from 'hooks/usePaginatedInputs';

import type { CollectorsConfig, Fleet, CollectorInstanceView, Source, CollectorStats } from '../types';
import { fetchCollectorsConfig } from './collectorsApi';

const QUERY_KEY_PREFIX = 'collectors';

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
  const url = PaginationURL('/collectors/fleets', searchParams.page, searchParams.pageSize, searchParams.query, {
    sort: searchParams.sort?.attributeId,
    order: searchParams.sort?.direction,
  });

  const response = await fetch('GET', qualifyUrl(url));

  return {
    list: response.elements,
    pagination: { total: response.total },
    attributes: response.attributes,
  };
};

// Sources paginated fetch
export const SOURCES_KEY_PREFIX = ['collectors', 'sources', 'paginated'];
export const sourcesKeyFn = (searchParams: SearchParams) => [...SOURCES_KEY_PREFIX, searchParams];

export const fetchPaginatedSources = async (
  searchParams: SearchParams,
  fleetId: string,
): Promise<PaginatedCollectorsResponse<Source>> => {
  const url = PaginationURL(`/collectors/fleets/${fleetId}/sources`, searchParams.page, searchParams.pageSize, searchParams.query, {
    sort: searchParams.sort?.attributeId,
    order: searchParams.sort?.direction,
  });

  const response = await fetch('GET', qualifyUrl(url));

  return {
    list: response.elements,
    pagination: { total: response.total },
    attributes: response.attributes,
  };
};

export const useCollectorStats = () =>
  useQuery<CollectorStats>({
    queryKey: [QUERY_KEY_PREFIX, 'stats'],
    queryFn: async () => fetch('GET', qualifyUrl('/collectors/stats')),
  });

export const useFleets = () =>
  useQuery<Fleet[]>({
    queryKey: [QUERY_KEY_PREFIX, 'fleets'],
    queryFn: async () => {
      const response = await fetch('GET', qualifyUrl('/collectors/fleets?per_page=0'));

      return response.elements;
    },
  });

export const useFleet = (fleetId: string) =>
  useQuery<Fleet>({
    queryKey: [QUERY_KEY_PREFIX, 'fleets', fleetId],
    queryFn: () => fetch('GET', qualifyUrl(`/collectors/fleets/${fleetId}`)),
    enabled: !!fleetId,
  });

export const useFleetStats = (fleetId: string) =>
  useQuery({
    queryKey: [QUERY_KEY_PREFIX, 'fleets', fleetId, 'stats'],
    queryFn: () => fetch('GET', qualifyUrl(`/collectors/fleets/${fleetId}/stats`)),
    enabled: !!fleetId,
  });

export const useInstances = (fleetId?: string) =>
  useQuery<CollectorInstanceView[]>({
    queryKey: [QUERY_KEY_PREFIX, 'instances', { fleetId }],
    queryFn: async () => {
      const url = fleetId
        ? `/collectors?fleet_id=${encodeURIComponent(fleetId)}&per_page=0`
        : '/collectors?per_page=0';
      const response = await fetch('GET', qualifyUrl(url));

      return (response.elements as ApiInstanceResponse[]).map(toView);
    },
  });

export const useSources = (fleetId?: string) =>
  useQuery<Source[]>({
    queryKey: [QUERY_KEY_PREFIX, 'sources', { fleetId }],
    queryFn: async () => {
      if (!fleetId) return [];
      const response = await fetch('GET', qualifyUrl(`/collectors/fleets/${fleetId}/sources?per_page=0`));

      return response.elements;
    },
    enabled: !!fleetId,
  });

export const COLLECTORS_CONFIG_KEY = [QUERY_KEY_PREFIX, 'config'];

export const useCollectorsConfig = () =>
  useQuery<CollectorsConfig>({
    queryKey: COLLECTORS_CONFIG_KEY,
    queryFn: fetchCollectorsConfig,
  });

export const useInput = (inputId: string | null) =>
  useQuery<InputSummary>({
    queryKey: ['inputs', inputId],
    queryFn: () => SystemInputs.get(inputId) as Promise<InputSummary>,
    enabled: !!inputId,
  });
