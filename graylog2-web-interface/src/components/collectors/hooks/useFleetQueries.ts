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

import { CollectorsFleets } from '@graylog/server-api';

import type { SearchParams } from 'stores/PaginationTypes';
import { defaultOnError } from 'util/conditional/onError';
import type { PaginatedResponse } from 'components/common/PaginatedEntityTable/useFetchEntities';

import type { BulkFleetStatsResponse, Fleet } from '../types';

export const FLEETS_KEY_PREFIX = ['collectors', 'fleets'];
export const fleetsKeyFn = (searchParams: SearchParams) => [...FLEETS_KEY_PREFIX, 'paginated', searchParams];

export const useFleets = () =>
  useQuery<Fleet[]>({
    queryKey: [...FLEETS_KEY_PREFIX],
    queryFn: () =>
      defaultOnError(
        CollectorsFleets.list(1, 0).then((response) => response.elements),
        'Loading fleets failed with status',
        'Could not load Fleets',
      ),
  });

export const useFleet = (fleetId: string) =>
  useQuery<Fleet>({
    queryKey: [...FLEETS_KEY_PREFIX, fleetId],
    queryFn: () =>
      defaultOnError(CollectorsFleets.get(fleetId), 'Loading fleet failed with status', 'Could not load Fleet'),
    enabled: !!fleetId,
  });

export const useFleetStats = (fleetId: string) =>
  useQuery({
    queryKey: [...FLEETS_KEY_PREFIX, fleetId, 'stats'],
    queryFn: () =>
      defaultOnError(
        CollectorsFleets.stats(fleetId),
        'Loading fleet stats failed with status',
        'Could not load Fleet Stats',
      ),
    enabled: !!fleetId,
  });

export const FLEETS_BULK_STATS_KEY = ['collectors', 'fleets', 'stats'];

const fetchBulkFleetStats = (): Promise<BulkFleetStatsResponse> => CollectorsFleets.bulkStats();

export const useFleetsBulkStats = () =>
  useQuery<BulkFleetStatsResponse>({
    queryKey: FLEETS_BULK_STATS_KEY,
    queryFn: () =>
      defaultOnError(fetchBulkFleetStats(), 'Loading fleet stats failed with status', 'Could not load Fleet Stats'),
  });

export const fetchPaginatedFleets = async (searchParams: SearchParams): Promise<PaginatedResponse<Fleet>> =>
  defaultOnError(
    CollectorsFleets.list(
      searchParams.page,
      searchParams.pageSize,
      searchParams.query,
      searchParams.sort?.attributeId,
      searchParams.sort?.direction,
    ).then((response) => ({
      list: response.elements,
      pagination: response.pagination,
      attributes: response.attributes,
    })),
    'Loading fleets failed with status',
    'Could not load Fleets',
  );
