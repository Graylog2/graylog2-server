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

import type { SearchParams } from 'stores/PaginationTypes';
import fetch from 'logic/rest/FetchProvider';
import * as URLUtils from 'util/URLUtils';

const url = URLUtils.qualifyUrl('/events/search');
export const keyFn = (searchParams: SearchParams) => ['events', 'search', searchParams];
const fetchEvents = (searchParams: SearchParams, timerange) => fetch('POST', url, {
  query: searchParams.query,
  page: searchParams.page,
  per_page: searchParams.pageSize,
  filter: searchParams.filters,
  timerange: {
    type: 'relative',
    range: 3369600,
  },
}).then(({ events, total_events, parameters, ...rest }) => {
  console.log({ events, rest, total_events, parameters });

  return ({
    ...rest,
    list: events.map(({ event }) => event),
    total: total_events,
    pagination: { total: total_events, page: parameters.page, per_page: parameters.per_page, count: events.length },
    query: searchParams.query,
  });
});

export default fetchEvents;
