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

import { DEFAULT_RANGE_TYPE } from 'views/Constants';
import {
  AbsoluteTimeRange,
  KeywordTimeRange,
  filtersForQuery,
  RelativeTimeRange,
  TimeRange,
  FilterType,
  ElasticsearchQueryString,
  createElasticsearchQueryString,
} from 'views/logic/queries/Query';

type RawRelativeRangeStartOnly = {
  rangetype: 'relative',
  relative?: string,
};

type RawRelativeRangeWithEnd = {
  rangetype: 'relative',
  from: string,
  to?: string,
};

type RawRelativeRange = RawRelativeRangeStartOnly | RawRelativeRangeWithEnd

type RawAbsoluteRange = {
  rangetype: 'absolute',
  from?: string,
  to?: string,
};

type RawKeywordRange = {
  rangetype: 'keyword',
  keyword?: string,
  timezone?: string,
};

type RawTimeRange = RawAbsoluteRange | RawRelativeRange | RawKeywordRange;

type StreamsQuery = {
  streams?: string,
};

export type RawQuery = Partial<RawTimeRange> & StreamsQuery & { q?: string };

const _getRange = (query): RawTimeRange => {
  const rangetype = query.rangetype || DEFAULT_RANGE_TYPE;

  return { ...query, rangetype };
};

const normalizeRawRelativeTimeRange = (range: RawRelativeRange): RelativeTimeRange | undefined => {
  const parseRangeValue = (rangeValue: string) => parseInt(rangeValue, 10);

  if ('relative' in range) {
    return { type: 'relative', range: parseRangeValue(range.relative) };
  }

  if ('from' in range) {
    const result = { type: 'relative' as const, from: parseRangeValue(range.from) };

    if ('to' in range) {
      return { ...result, to: parseRangeValue(range.to) };
    }

    return result;
  }

  return undefined;
};

const normalizeTimeRange = (query: Partial<RawTimeRange> = {}) => {
  const range = _getRange(query);

  switch (range.rangetype) {
    case 'relative':
      return normalizeRawRelativeTimeRange(range);
    case 'absolute':
      return (range.from || range.to) ? {
        type: range.rangetype,
        from: range.from,
        to: range.to,
      } as AbsoluteTimeRange : undefined;
    case 'keyword':
      return range.keyword ? {
        type: range.rangetype,
        keyword: range.keyword,
        timezone: range.timezone,
      } as KeywordTimeRange : undefined;
    default:
      // @ts-expect-error
      throw new Error(`Unsupported range type ${range.rangetype} in range: ${JSON.stringify(range)}`);
  }
};

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
