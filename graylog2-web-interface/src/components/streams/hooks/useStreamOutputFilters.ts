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

import UserNotification from 'util/UserNotification';
import ApiRoutes from 'routing/ApiRoutes';
import type { SearchParams } from 'stores/PaginationTypes';
import PaginationURL from 'util/PaginationURL';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import type { StreamOutputFilterRule } from 'components/streams/StreamDetails/output-filter/Types';

type PaginatedResponse = {
  total: number,
  page: number,
  per_page: number,
  count: number,
  elements: Array<StreamOutputFilterRule>,
  query: string
}
export const KEY_PREFIX = ['streams', 'output', 'filters'];
export const keyFn = (streamId: string, destinationType: string, searchParams?: SearchParams) => [...KEY_PREFIX, streamId, destinationType, searchParams];
const defaultParams = { page: 1, pageSize: 0, sort: undefined };

export const fetchStreamOutputFilters = async (streamId: string, searchParams: SearchParams) => {
  const url = PaginationURL(
    ApiRoutes.StreamOutputFilterRuleApiController.get(streamId).url,
    searchParams.page,
    searchParams.pageSize,
    searchParams.query,
    { sort: searchParams.sort?.attributeId, order: searchParams.sort?.direction });

  return fetch('GET', qualifyUrl(url)).then((response: PaginatedResponse) => {
    const {
      elements,
      query,
      total,
      page,
      per_page: perPage,
    } = response;

    return {
      list: elements,
      attributes: [],
      pagination: {
        total,
        page,
        perPage,
        query,
      },
    };
  });
};

const useStreamOutputFilters = (streamId: string, destinationType: string): {
  data: {
    list: Array<StreamOutputFilterRule>,
    pagination: { total: number }
  },
  refetch: () => void,
  isInitialLoading: boolean,
  isSuccess: boolean,
} => {
  const { data, refetch, isInitialLoading, isSuccess } = useQuery(
    keyFn(streamId, destinationType),
    () => fetchStreamOutputFilters(streamId, { ...defaultParams, query: `destination_type:${destinationType}` }),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading stream output filters failed with status: ${errorThrown}`,
          'Could not load stream output filters');
      },
      keepPreviousData: true,
    },
  );

  return ({
    data: data,
    refetch,
    isInitialLoading,
    isSuccess,
  });
};

export default useStreamOutputFilters;
