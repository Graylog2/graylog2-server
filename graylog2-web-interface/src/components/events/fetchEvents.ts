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

import moment from 'moment';
import trim from 'lodash/trim';

import * as URLUtils from 'util/URLUtils';
import { adjustFormat } from 'util/DateTime';
import type { SearchParams } from 'stores/PaginationTypes';
import fetch from 'logic/rest/FetchProvider';
import type { PaginatedResponse } from 'components/common/PaginatedEntityTable/useFetchEntities';
import type { Event, EventsAdditionalData } from 'components/events/events/types';
import { additionalAttributes } from 'components/events/Constants';
import { extractRangeFromString } from 'components/common/EntityFilters/helpers/timeRange';
import type { UrlQueryFilters } from 'components/common/EntityFilters/types';

const url = URLUtils.qualifyUrl('/events/search');

type FiltersResult = {
  filter: {
    alerts?: string,
    event_definitions?: Array<string>,
    priority?: Array<string>,
    aggregation_timerange?: { from?: string, to?: string, type: string, range?: number },
    key?: Array<string>,
  },
  timerange?: { from?: string, to?: string, type: string, range?: number },
};

const allTimesRange = { type: 'relative', range: 0 };

const isNullOrBlank = (s: string | undefined) => {
  if (!s) {
    return true;
  }

  if (trim(s) === '') {
    return true;
  }

  return false;
};

const parseTimestampFilter = (timestamp: string | undefined) => {
  if (!timestamp) {
    return allTimesRange;
  }

  const [from, to] = extractRangeFromString(timestamp);

  if (!from && !to) {
    return allTimesRange;
  }

  return {
    type: 'absolute',
    from: isNullOrBlank(from) ? adjustFormat(moment(0).utc(), 'internal') : from,
    to: isNullOrBlank(to) ? adjustFormat(moment().utc(), 'internal') : to,
  };
};

const parseFilters = (filters: UrlQueryFilters) => {
  const result: FiltersResult = { filter: {} };

  result.timerange = parseTimestampFilter(filters.get('timestamp')?.[0]);

  if (filters.get('timerange_start')?.[0]) {
    const [from, to] = extractRangeFromString(filters.get('timerange_start')[0]);

    result.filter.aggregation_timerange = from
      ? { from, to: to || adjustFormat(moment().utc(), 'internal'), type: 'absolute' }
      : { type: 'relative', range: 0 };
  }

  if (filters.get('key')?.length > 0) {
    result.filter.key = filters.get('key');
  }

  if (filters.get('event_definition_id')?.length > 0) {
    result.filter.event_definitions = filters.get('event_definition_id');
  }

  if (filters.get('priority')?.length > 0) {
    result.filter.priority = filters.get('priority');
  }

  switch (filters?.get('alert')?.[0]) {
    case 'true':
      result.filter.alerts = 'only';
      break;
    case 'false':
      result.filter.alerts = 'exclude';
      break;
    default:
      result.filter.alerts = 'include';
  }

  return result;
};

const getConcatenatedQuery = (query: string, streamId: string) => {
  if (!streamId) return query;

  if (streamId && !query) return `source_streams:${streamId}`;

  return `(${query}) AND source_streams:${streamId}`;
};

export const keyFn = (searchParams: SearchParams) => ['events', 'search', searchParams];

const fetchEvents = (searchParams: SearchParams, streamId: string): Promise<PaginatedResponse<Event, EventsAdditionalData>> => fetch('POST', url, {
  query: getConcatenatedQuery(searchParams.query, streamId),
  page: searchParams.page,
  per_page: searchParams.pageSize,
  sort_by: searchParams.sort.attributeId,
  sort_direction: searchParams.sort.direction,
  ...parseFilters(searchParams.filters),
}).then(({ events, total_events, parameters, context }) => ({
  attributes: additionalAttributes,
  list: events.map(({ event }) => event),
  pagination: { total: total_events, page: parameters.page, per_page: parameters.per_page, count: events.length },
  meta: {
    context,
  },
}));

export default fetchEvents;
