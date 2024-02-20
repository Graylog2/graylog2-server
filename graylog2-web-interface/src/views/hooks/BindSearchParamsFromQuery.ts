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
import type { ViewHook, ViewHookArguments } from 'views/logic/hooks/ViewHook';
import View from 'views/logic/views/View';
import normalizeSearchURLQueryParams from 'views/logic/NormalizeSearchURLQueryParams';
import createSearch from 'views/logic/slices/createSearch';

const bindSearchParamsFromQuery: ViewHook = async ({ query, view, executionState }: ViewHookArguments) => {
  if (view.type !== View.Type.Search) {
    return [view, executionState];
  }

  const { queryString, timeRange, streamsFilter } = normalizeSearchURLQueryParams(query);

  if (!queryString && !timeRange && !streamsFilter) {
    return [view, executionState];
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

  if (streamsFilter) {
    queryBuilder = queryBuilder.filter(streamsFilter);
  }

  const newQuery = queryBuilder.build();

  if (isDeepEqual(newQuery, firstQuery)) {
    return [view, executionState];
  }

  const newSearch = view.search.toBuilder().newId().queries([newQuery]).build();

  const savedSearch = await createSearch(newSearch);

  const newView = view.toBuilder().search(savedSearch).build();

  return [newView, executionState];
};

export default bindSearchParamsFromQuery;
