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
import { defaultOnError } from 'util/conditional/onError';

import type { CollectorInstanceView } from '../types';

export type PaginatedCollectorsResponse<T> = {
  list: T[];
  pagination: { total: number };
  attributes: Attribute[];
};

export const INSTANCES_KEY_PREFIX = ['collectors', 'instances'];
export const instancesKeyFn = (searchParams: SearchParams) => [...INSTANCES_KEY_PREFIX, 'paginated', searchParams];

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
  const allAttributes = { ...dto.identifying_attributes, ...dto.non_identifying_attributes };

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
  const url = PaginationURL('/collectors', searchParams.page, searchParams.pageSize, searchParams.query);

  return defaultOnError(
    fetch('GET', qualifyUrl(url)).then((response) => ({
      list: (response.elements as ApiInstanceResponse[]).map(toView),
      pagination: response.pagination,
      attributes: INSTANCE_ATTRIBUTES,
    })),
    'Loading instances failed with status',
    'Could not load instances',
  );
};

export const useInstances = (fleetId?: string) =>
  useQuery<CollectorInstanceView[]>({
    queryKey: [...INSTANCES_KEY_PREFIX, { fleetId }],
    queryFn: async () => {
      const url = fleetId
        ? `/collectors?fleet_id=${encodeURIComponent(fleetId)}&per_page=0`
        : '/collectors?per_page=0';

      return defaultOnError(
        fetch('GET', qualifyUrl(url))
          .then((response) => (response.elements as ApiInstanceResponse[]).map(toView)),
        'Loading collector instances failed with status',
        'Could not load collector instances',
      );
    },
  });
