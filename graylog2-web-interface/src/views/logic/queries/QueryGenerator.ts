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
import { OrderedMap } from 'immutable';
import { v4 as uuidv4 } from 'uuid';

import { DEFAULT_TIMERANGE } from 'views/Constants';
import type { TimeRange, ElasticsearchQueryString, QueryId, FilterType } from 'views/logic/queries/Query';
import Query, { createElasticsearchQueryString, filtersForQuery } from 'views/logic/queries/Query';
import generateId from 'logic/generateId';
import type { SearchFilter } from 'components/event-definitions/event-definitions-types';

export default (
  streamId?: string | string[],
  // eslint-disable-next-line default-param-last
  id: QueryId | undefined = generateId(),
  timeRange?: TimeRange,
  queryString?: ElasticsearchQueryString,
  searchFilters?: SearchFilter[],
): Query => {
  // eslint-disable-next-line no-nested-ternary
  const streamIds = streamId
    ? streamId instanceof Array
      ? streamId
      : [streamId]
    : null;
  const streamFilter = filtersForQuery(streamIds);
  const searchFiltersMap: FilterType = searchFilters
    ? OrderedMap(searchFilters?.map((filter) => [filter.id || uuidv4(), filter]))
    : OrderedMap();
  const builder = Query.builder()
    .id(id)
    .query(queryString ?? createElasticsearchQueryString())
    .timerange(timeRange ?? DEFAULT_TIMERANGE)
    .filters(searchFiltersMap.toList());

  return streamFilter ? builder.filter(streamFilter).build() : builder.build();
};
