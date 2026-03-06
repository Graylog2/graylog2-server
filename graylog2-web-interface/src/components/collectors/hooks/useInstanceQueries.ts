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

import { Collectors } from '@graylog/server-api';

import type { SearchParams, Attribute } from 'stores/PaginationTypes';
import FiltersForQueryParams from 'components/common/EntityFilters/FiltersForQueryParams';
import { defaultOnError } from 'util/conditional/onError';

import type { CollectorInstanceView } from '../types';

export type PaginatedCollectorsResponse<T> = {
  list: T[];
  pagination: { total: number };
  attributes: Attribute[];
};

export const INSTANCES_KEY_PREFIX = ['collectors', 'instances'];
export const instancesKeyFn = (searchParams: SearchParams) => [...INSTANCES_KEY_PREFIX, 'paginated', searchParams];

type ApiInstanceResponse = Awaited<ReturnType<typeof Collectors.findInstances>>['elements'][number];

const toView = (dto: ApiInstanceResponse): CollectorInstanceView => {
  const allAttributes = { ...dto.identifying_attributes, ...dto.non_identifying_attributes };

  return ({
    id: dto.instance_uid,
    instance_uid: dto.instance_uid,
    fleet_id: dto.fleet_id,
    capabilities: dto.capabilities,
    enrolled_at: dto.enrolled_at,
    last_seen: dto.last_seen,
    status: dto.status as CollectorInstanceView['status'],
    certificate_fingerprint: dto.certificate_fingerprint,
    identifying_attributes: dto.identifying_attributes ?? {},
    non_identifying_attributes: dto.non_identifying_attributes ?? {},
    hostname: (allAttributes?.['host.name'] as string) ?? null,
    os: (allAttributes?.['os.type'] as string) ?? null,
    version: (allAttributes?.['service.version'] as string) ?? null,
  });
};

export const fetchPaginatedInstances = async (
  searchParams: SearchParams,
): Promise<PaginatedCollectorsResponse<CollectorInstanceView>> =>
  defaultOnError(
    Collectors.findInstances(
      searchParams.page,
      searchParams.pageSize,
      searchParams.query,
      FiltersForQueryParams(searchParams.filters),
      searchParams.sort?.attributeId as 'instance_uid' | 'last_seen',
      searchParams.sort?.direction,
    ).then((response) => ({
      list: response.elements.map(toView),
      pagination: response.pagination,
      attributes: response.attributes,
    })),
    'Loading instances failed with status',
    'Could not load instances',
  );

export const useInstances = (fleetId?: string) =>
  useQuery<CollectorInstanceView[]>({
    queryKey: [...INSTANCES_KEY_PREFIX, { fleetId }],
    queryFn: () => {
      const filters = fleetId ? [`fleet_id:${fleetId}`] : undefined;

      return defaultOnError(
        Collectors.findInstances(1, 0, undefined, filters)
          .then((response) => response.elements.map(toView)),
        'Loading collector instances failed with status',
        'Could not load collector instances',
      );
    },
  });
