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
import { keepPreviousData, useQuery } from '@tanstack/react-query';

import { SystemNotifications } from '@graylog/server-api';

import type { SearchParams } from 'stores/PaginationTypes';
import FiltersForQueryParams from 'components/common/EntityFilters/FiltersForQueryParams';
import type { PaginatedResponse } from 'components/common/PaginatedEntityTable/useFetchEntities';
import type { NotificationType } from 'components/notifications/types';
import { NOTIFICATIONS_QUERY_KEY, TABLE_KEY } from 'components/notifications/constants';

export const keyFn = (searchParams: SearchParams) => [...NOTIFICATIONS_QUERY_KEY, TABLE_KEY, searchParams];

type SortField = Parameters<typeof SystemNotifications.getPaginated>[4];

const EMPTY_PAGE: PaginatedResponse<NotificationType> = { list: [], pagination: { total: 0 }, attributes: [] };

export const fetchNotifications = (searchParams: SearchParams): Promise<PaginatedResponse<NotificationType>> =>
  SystemNotifications.getPaginated(
    searchParams.page,
    searchParams.pageSize,
    searchParams.query,
    searchParams.filters ? FiltersForQueryParams(searchParams.filters) : undefined,
    searchParams.sort.attributeId as SortField,
    searchParams.sort.direction,
  )
    .then(({ elements, pagination, attributes }) => ({
      list: elements as NotificationType[],
      pagination,
      attributes,
    }))
    .catch((err: { status?: number }) => {
      if (err?.status === 404) return EMPTY_PAGE;
      throw err;
    });

const useNotificationsList = (searchParams: SearchParams, { enabled = true }: { enabled?: boolean } = {}) => {
  const { data, isLoading, refetch } = useQuery({
    queryKey: keyFn(searchParams),
    queryFn: () => fetchNotifications(searchParams),
    placeholderData: keepPreviousData,
    retry: false,
    enabled,
  });

  return {
    data,
    isLoading,
    refetch,
  };
};

export default useNotificationsList;
