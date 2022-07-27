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

import type {
  TimeRange,
  FilterType,
  ElasticsearchQueryString,
} from 'views/logic/queries/Query';
import {
  filtersForQuery,
  createElasticsearchQueryString,
} from 'views/logic/queries/Query';
import type { SearchTimeRange } from 'views/logic/TimeRange';
import { fromSearchTimerange } from 'views/logic/TimeRange';
import { DEFAULT_RANGE_TYPE } from 'views/Constants';

type StreamsQuery = {
  streams?: string,
};

export type RawQuery = (SearchTimeRange | { relative?: string }) & StreamsQuery & { q?: string };

// eslint-disable-next-line no-nested-ternary
const normalizeTimeRange = (query: {} | SearchTimeRange): TimeRange | undefined => (query && 'rangetype' in query
  ? fromSearchTimerange(query)
  : 'relative' in query
    ? fromSearchTimerange({ ...query, rangetype: DEFAULT_RANGE_TYPE })
    : undefined);

const normalizeStreams = (query: StreamsQuery = {}): Array<string> => {
  const rawStreams = query.streams;

  if (rawStreams === undefined || rawStreams === null) {
    return [];
  }

  return String(rawStreams).split(',')
    .map((stream) => stream.trim())
    .filter((stream) => stream !== '');
};

type NormalizedSearchURLQueryParams = {
  timeRange: TimeRange | undefined,
  streamsFilter: FilterType | undefined,
  queryString: ElasticsearchQueryString | undefined
}

const normalizeSearchURLQueryParams = (query: RawQuery): NormalizedSearchURLQueryParams => {
  const { q: queryString } = query ?? {};
  const timeRange = normalizeTimeRange(query);
  const streamsFilter = filtersForQuery(normalizeStreams(query));

  return {
    timeRange,
    streamsFilter,
    queryString: queryString ? createElasticsearchQueryString(queryString) : undefined,
  };
};

export default normalizeSearchURLQueryParams;
