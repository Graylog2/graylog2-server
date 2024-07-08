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
import { StreamDestinationsFilters } from '@graylog/server-api';
import type { SearchParams } from 'stores/PaginationTypes';

import type { StreamOutputFilterRule } from '../StreamDetails/common/Types';

type PaginatedResponse = {
  total: number,
  page: number,
  per_page: number,
  count: number,
  elements: Array<StreamOutputFilterRule>,
  query: string
}
export const KEY_PREFIX = ['streams', 'overview'];
export const keyFn = (searchParams: SearchParams) => [...KEY_PREFIX, searchParams];

export const fetchStreamOutputFilters = (streamId: string, _searchParams: SearchParams) => {
  // const paginationParam = {
  //   per_page: searchParams?.pageSize,
  //   query: searchParams?.query,
  //   page: searchParams?.page,
  //   sort_by: searchParams?.sort.attributeId,
  //   order: searchParams?.sort.direction,
  // };

  return StreamDestinationsFilters.getPaginatedFiltersForStream(streamId).then((response: PaginatedResponse) => {
    const {
      elements,
      query,
      total,
      page,
      per_page: perPage,
    } = response;

    return {
      list: elements,
      pagination: {
        total,
        page,
        perPage,
        query,
      },
    };
  });
};

export default fetchStreamOutputFilters;
