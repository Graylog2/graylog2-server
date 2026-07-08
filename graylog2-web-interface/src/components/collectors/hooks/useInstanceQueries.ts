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
import { hashKey, useQuery } from '@tanstack/react-query';

import { Collectors } from '@graylog/server-api';

import type { SearchParams } from 'stores/PaginationTypes';
import FiltersForQueryParams from 'components/common/EntityFilters/FiltersForQueryParams';
import { defaultOnError } from 'util/conditional/onError';
import type { PaginatedResponse } from 'components/common/PaginatedEntityTable/useFetchEntities';
import type { RequestOptions } from 'routing/request';

import type { CollectorInstanceView } from '../types';

const NO_SESSION_EXT: RequestOptions = { requestShouldExtendSession: false };
export const INSTANCES_KEY_PREFIX = ['collectors', 'instances'];
export const instancesKeyFn = (searchParams: SearchParams) => [...INSTANCES_KEY_PREFIX, 'paginated', searchParams];

type ApiInstanceResponse = Awaited<ReturnType<typeof Collectors.findInstances>>['elements'][number];

const toView = (dto: ApiInstanceResponse): CollectorInstanceView => {
  const allAttributes = { ...dto.identifying_attributes, ...dto.non_identifying_attributes };

  return {
    id: dto.instance_uid,
    instance_uid: dto.instance_uid,
    fleet_id: dto.fleet_id,
    capabilities: dto.capabilities,
    enrolled_at: dto.enrolled_at,
    last_seen: dto.last_seen,
    status: dto.status as CollectorInstanceView['status'],
    active_certificate_fingerprint: dto.active_certificate_fingerprint,
    active_certificate_expires_at: dto.active_certificate_expires_at,
    next_certificate_fingerprint: dto.next_certificate_fingerprint,
    next_certificate_expires_at: dto.next_certificate_expires_at,
    identifying_attributes: dto.identifying_attributes ?? {},
    non_identifying_attributes: dto.non_identifying_attributes ?? {},
    hostname: (allAttributes?.['host.name'] as string) ?? null,
    os: (allAttributes?.['os.type'] as string) ?? null,
    version: (allAttributes?.['service.version'] as string) ?? null,
    has_pending_changes: dto.has_pending_changes,
  };
};

// The instances table refetches both on user interaction and on a fixed interval, but react-query
// doesn't tell the query function what triggered a fetch. We infer it instead: interactions
// (paging, sorting, filtering, searching) always change the search params, while an interval
// refresh repeats the previous ones — so a repeated-params fetch is a background refresh and must
// not keep the session alive, whereas a user actively working with the table extends it as usual.
//
// Caveat:
// - Returning to the page via SPA navigation with unchanged params looks like a background refresh
//   (this module-level state outlives the component). The page's sibling queries (fleets, config)
//   don't opt out of session extension, so the navigation still extends the session through them.
let lastFetchedParamsHash: string | null = null;

const isBackgroundRefresh = (searchParams: SearchParams): boolean => {
  const paramsHash = hashKey(instancesKeyFn(searchParams));
  const isRepeatedFetch = paramsHash === lastFetchedParamsHash;
  lastFetchedParamsHash = paramsHash;

  return isRepeatedFetch;
};

export const fetchPaginatedInstances = async (
  searchParams: SearchParams,
): Promise<PaginatedResponse<CollectorInstanceView>> =>
  defaultOnError(
    Collectors.findInstances(
      searchParams.page,
      searchParams.pageSize,
      searchParams.query,
      FiltersForQueryParams(searchParams.filters),
      searchParams.sort?.attributeId as 'instance_uid' | 'last_seen',
      searchParams.sort?.direction,
      { requestShouldExtendSession: !isBackgroundRefresh(searchParams) },
    ).then((response) => ({
      list: response.elements.map(toView),
      pagination: response.pagination,
      attributes: response.attributes,
    })),
    'Loading instances failed with status',
    'Could not load instances',
  );

export const useInstances = (fleetId?: string, options: { refetchInterval?: number; silent?: boolean } = {}) =>
  useQuery<CollectorInstanceView[]>({
    // eslint-disable-next-line @tanstack/query/exhaustive-deps -- silent only affects the error-reporting wrapper, not the cached data; callers deliberately share one cache entry regardless of the flag
    queryKey: [...INSTANCES_KEY_PREFIX, { fleetId }],
    queryFn: () => {
      const filters = fleetId ? [`fleet_id:${fleetId}`] : undefined;
      const promise = Collectors.findInstances(1, 0, undefined, filters, undefined, undefined, NO_SESSION_EXT).then(
        (response) => response.elements.map(toView),
      );

      return options.silent
        ? promise
        : defaultOnError(
            promise,
            'Loading Collector instances failed with status',
            'Could not load Collector instances',
          );
    },
    refetchInterval: options.refetchInterval,
  });

export const useInstance = (instanceUid: string | undefined) => {
  const { data, isLoading, error, isError } = useQuery<CollectorInstanceView>({
    queryKey: [...INSTANCES_KEY_PREFIX, 'single', instanceUid],
    queryFn: () =>
      defaultOnError(
        Collectors.getInstance(instanceUid).then((response) => toView(response)),
        'Loading Collector instance failed with status',
        'Could not load Collector instance',
      ),
    enabled: !!instanceUid,
  });

  return { data, isLoading, error, isError };
};
