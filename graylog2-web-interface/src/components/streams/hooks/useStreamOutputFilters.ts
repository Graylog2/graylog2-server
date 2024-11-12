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
import * as Immutable from 'immutable';

import PaginationURL from 'util/PaginationURL';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import type { PaginatedList, Pagination } from 'stores/PaginationTypes';
import fetch from 'logic/rest/FetchProvider';
import type { StreamOutputFilterRule } from 'components/streams/StreamDetails/output-filter/Types';
import { defaultOnError } from 'util/conditional/onError';

type PaginatedResponse = {
  total: number,
  page: number,
  per_page: number,
  count: number,
  elements: Array<StreamOutputFilterRule>,
  query: string
}
export const KEY_PREFIX = ['streams', 'output', 'filters'];
export const keyFn = (streamId: string, destinationType: string, pagination?: Pagination) => [...KEY_PREFIX, streamId, destinationType, pagination];
const defaultParams = { page: 1, perPage: 10, query: '' };

export const fetchStreamOutputFilters = async (streamId: string, pagination: Pagination) => {
  const url = PaginationURL(
    ApiRoutes.StreamOutputFilterRuleApiController.get(streamId).url,
    pagination.page,
    pagination.perPage,
    pagination.query,
  );

  return fetch('GET', qualifyUrl(url)).then((response: PaginatedResponse) => {
    const {
      elements,
      query,
      total,
      page,
      per_page: perPage,
      count,
    } = response;

    return {
      list: Immutable.List(elements),
      attributes: [],
      pagination: {
        total,
        page,
        perPage,
        query,
        count,
      },
    };
  });
};

const useStreamOutputFilters = (streamId: string, destinationType: string, pagination: Pagination = defaultParams): {
  data: PaginatedList<StreamOutputFilterRule>,
  refetch: () => void,
  isLoading: boolean,
  isSuccess: boolean,
} => {
  const { data, refetch, isLoading, isSuccess } = useQuery(
    keyFn(streamId, destinationType, pagination),
    () => defaultOnError(fetchStreamOutputFilters(streamId, { ...pagination, query: `destination_type:${destinationType}` }),
      'Loading stream output filters failed with status',
      'Could not load stream output filters'),
    {
      keepPreviousData: true,
    },
  );

  return ({
    data,
    refetch,
    isLoading,
    isSuccess,
  });
};

export default useStreamOutputFilters;
