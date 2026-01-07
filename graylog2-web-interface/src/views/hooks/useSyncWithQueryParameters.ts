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
import { useEffect, useRef } from 'react';
import * as Immutable from 'immutable';
import URI from 'urijs';

import type { ViewType } from 'views/logic/views/View';
import View from 'views/logic/views/View';
import type { TimeRange } from 'views/logic/queries/Query';
import type Query from 'views/logic/queries/Query';
import { filtersToStreamSet, filtersToStreamCategorySet } from 'views/logic/queries/Query';
import { isTypeRelativeWithStartOnly, isTypeRelativeWithEnd } from 'views/typeGuards/timeRange';
import useViewType from 'views/hooks/useViewType';
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';
import useHistory from 'routing/useHistory';

const extractTimerangeParams = (timerange: TimeRange): [string, string | number][] => {
  const { type } = timerange;
  const result = { rangetype: type };

  const formatResult = (paramsObject): [string, string | number][] => Object.entries(paramsObject);

  switch (timerange.type) {
    case 'relative':
      if (isTypeRelativeWithStartOnly(timerange)) {
        return formatResult({ ...result, relative: timerange.range });
      }

      if (isTypeRelativeWithEnd(timerange)) {
        if ('to' in timerange) {
          return formatResult({ ...result, from: timerange.from, to: timerange.to });
        }

        return formatResult({ ...result, from: timerange.from });
      }

      return formatResult(result);
    case 'keyword':
      return formatResult({ ...result, keyword: timerange.keyword });
    case 'absolute':
      return formatResult({ ...result, from: timerange.from, to: timerange.to });
    default:
      return Object.entries(result);
  }
};

const uriForView = (viewType: ViewType, query: string, searchQuery: Query) => {
  if (viewType !== View.Type.Search) {
    return undefined;
  }

  if (searchQuery) {
    const {
      query: { query_string: queryString },
      timerange,
      filter = Immutable.Map(),
    } = searchQuery;
    const baseUri = new URI(query)
      .setSearch('q', queryString)
      .removeQuery('from')
      .removeQuery('to')
      .removeQuery('keyword')
      .removeQuery('relative');
    const uriWithTimerange = extractTimerangeParams(timerange).reduce(
      (prev, [key, value]) => prev.setSearch(key, String(value)),
      baseUri,
    );
    const currentStreams = filtersToStreamSet(filter);
    const currentStreamCategories = filtersToStreamCategorySet(filter);

    const uriWithStreams = currentStreams.isEmpty()
      ? uriWithTimerange.removeSearch('streams')
      : uriWithTimerange.setSearch('streams', currentStreams.join(','));

    const uri = currentStreamCategories.isEmpty()
      ? uriWithStreams.removeSearch('stream_categories')
      : uriWithStreams.setSearch('stream_categories', currentStreamCategories.join(','));

    return uri.toString();
  }

  return undefined;
};

function canonicalizeUri(uri: string) {
  const [path, queryString = ''] = uri.split('?', 2);
  if (!queryString) return path;

  const params = queryString
    .split('&')
    .filter(Boolean)
    .map((part) => {
      const [k, v = ''] = part.split('=', 2);

      return [k, v];
    })
    .sort(([k1, v1], [k2, v2]) => (k1 === k2 ? v1.localeCompare(v2) : k1.localeCompare(k2)));

  const canonicalQuery = params.map(([k, v]) => `${k}=${v}`).join('&');

  return `${path}?${canonicalQuery}`;
}

function isSameUri(a: string, b: string) {
  return canonicalizeUri(a) === canonicalizeUri(b);
}

// Update the URL query parameters when the current view changes.
const useSyncWithQueryParameters = (currentUri: string) => {
  const viewType = useViewType();
  const currentQuery = useCurrentQuery();
  const history = useHistory();
  const lastSyncedUriRef = useRef(currentUri);
  const isFirstSyncRef = useRef(true);

  useEffect(() => {
    const uriForCurrentView = uriForView(viewType, currentUri, currentQuery);

    if (!uriForCurrentView) {
      return;
    }

    const currentUriMatchesCurrentView = isSameUri(currentUri, uriForCurrentView);
    const currentUriMatchesLastSync = isSameUri(currentUri, lastSyncedUriRef.current);

    // Don't update the URI if it was changed outside of this sync. For example when using the browser navigation.
    if (!currentUriMatchesLastSync && !currentUriMatchesCurrentView) {
      return;
    }

    if (!currentUriMatchesCurrentView) {
      const updateHistory = isFirstSyncRef.current ? history.replace : history.push;
      updateHistory(uriForCurrentView);
    }

    isFirstSyncRef.current = false;
    lastSyncedUriRef.current = uriForCurrentView;
  }, [currentQuery, history, currentUri, viewType]);
};

export default useSyncWithQueryParameters;
