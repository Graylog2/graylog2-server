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

import { CollectorsSources } from '@graylog/server-api';

import type { SearchParams } from 'stores/PaginationTypes';
import { defaultOnError } from 'util/conditional/onError';

import type { PaginatedResponse } from 'components/common/PaginatedEntityTable/useFetchEntities';

import type { Source } from '../types';

export const SOURCES_KEY_PREFIX = ['collectors', 'sources'];
export const sourcesKeyFn = (searchParams: SearchParams) => [...SOURCES_KEY_PREFIX, 'paginated', searchParams];

export const fetchPaginatedSources = async (
  searchParams: SearchParams,
  fleetId: string,
): Promise<PaginatedResponse<Source>> =>
  defaultOnError(
    CollectorsSources.list(
      fleetId,
      searchParams.page,
      searchParams.pageSize,
      searchParams.query,
      searchParams.sort?.attributeId,
      searchParams.sort?.direction,
    ).then((response) => ({
      list: response.elements as unknown as Source[],
      pagination: response.pagination,
      attributes: response.attributes,
    })),
    'Loading sources failed with status',
    'Could not load sources',
  );

export const useSources = (fleetId?: string) =>
  useQuery<Source[]>({
    queryKey: [...SOURCES_KEY_PREFIX, { fleetId }],
    queryFn: () => {
      if (!fleetId) return [];

      return defaultOnError(
        CollectorsSources.list(fleetId, 1, 0).then((response) => response.elements as unknown as Source[]),
        'Loading sources failed with status',
        'Could not load sources',
      );
    },
    enabled: !!fleetId,
  });
