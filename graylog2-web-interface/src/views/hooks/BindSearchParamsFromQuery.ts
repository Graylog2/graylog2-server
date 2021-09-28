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
import isDeepEqual from 'stores/isDeepEqual';
import { QueriesActions } from 'views/stores/QueriesStore';
import { ViewHook } from 'views/logic/hooks/ViewHook';
import View from 'views/logic/views/View';
import normalizeSearchURLQueryParams, { RawQuery } from 'views/logic/NormalizeSearchURLQueryParmas';

const bindSearchParamsFromQuery: ViewHook = ({ query, view }: {query: RawQuery, view: View }) => {
  if (view.type !== View.Type.Search) {
    return Promise.resolve(true);
  }

  const { queryString, timeRange, streams } = normalizeSearchURLQueryParams(query);

  if (!queryString && !timeRange && !streams) {
    return Promise.resolve(true);
  }

  const { queries } = view.search;

  if (queries.size !== 1) {
    throw new Error('Searches must only have a single query!');
  }

  const firstQuery = queries.first();
  let queryBuilder = firstQuery.toBuilder();

  if (queryString) {
    queryBuilder = queryBuilder.query(queryString);
  }

  if (timeRange) {
    queryBuilder = queryBuilder.timerange(timeRange);
  }

  if (streams) {
    queryBuilder = queryBuilder.filter(streams);
  }

  const newQuery = queryBuilder.build();

  return isDeepEqual(newQuery, firstQuery)
    ? Promise.resolve(true)
    : QueriesActions.update(firstQuery.id, queryBuilder.build())
      .then(() => true, () => false);
};

export default bindSearchParamsFromQuery;
