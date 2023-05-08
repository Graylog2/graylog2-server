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
import { createSelector } from '@reduxjs/toolkit';

import type { RootState } from 'views/types';
import { selectGlobalOverride, selectSearchExecutionResult } from 'views/logic/slices/searchExecutionSelectors';
import View from 'views/logic/views/View';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';

export const selectRootView = (state: RootState) => state.view;
export const selectView = createSelector(selectRootView, (state) => state.view);
export const selectActiveQuery = createSelector(selectRootView, (state) => state.activeQuery);
export const selectIsDirty = createSelector(selectRootView, (state) => state.isDirty);
export const selectIsNew = createSelector(selectRootView, (state) => state.isNew);
export const selectViewType = createSelector(selectView, (view) => view.type);
export const selectViewStates = createSelector(selectView, (state) => state.state);
export const selectViewState = (id: string) => createSelector(selectViewStates, (states) => states.get(id));
export const selectActiveViewState = createSelector(
  selectActiveQuery,
  selectViewStates,
  (activeQuery, states) => states?.get(activeQuery),
);
export const selectSearch = createSelector(selectView, (view) => view.search);
export const selectSearchId = createSelector(selectSearch, (search) => search.id);
export const selectSearchQueries = createSelector(selectSearch, (search) => search.queries);
export const selectSearchQuery = (queryId: string) => createSelector(selectSearchQueries, (queries) => queries.find((q) => q.id === queryId));
export const selectCurrentQuery = createSelector(
  selectActiveQuery,
  selectSearchQueries,
  (activeQuery, queries) => queries.find((query) => query.id === activeQuery),
);
export const selectQueryById = (queryId: string) => createSelector(
  selectSearchQueries,
  (queries) => queries.find((query) => query.id === queryId),
);

export const selectWidgets = createSelector(selectActiveViewState, (viewState) => viewState.widgets);
export const selectWidget = (widgetId: string) => createSelector(selectWidgets, (widgets) => widgets.find((widget) => widget.id === widgetId));
export const selectTitles = createSelector(selectActiveViewState, (viewState) => viewState.titles);

const selectQueryStringFromQuery = (queryId: string) => createSelector(selectSearch, (search) => (
  search.queries.find((q) => q.id === queryId).query.query_string
));

const selectQueryStringFromGlobalOverride = createSelector(selectGlobalOverride, (globalOverride) => {
  const { query_string } = globalOverride?.query ?? createElasticsearchQueryString();

  return query_string;
});

export const selectQueryString = (queryId: string) => createSelector(
  selectViewType,
  selectQueryStringFromQuery(queryId),
  selectQueryStringFromGlobalOverride,
  (viewType, queryQueryString, globalOverrideQueryString) => (
    viewType === View.Type.Search
      ? queryQueryString
      : globalOverrideQueryString
  ),
);

export const selectParameters = createSelector(selectSearch, (search) => search.parameters);
export const selectCurrentQueryResults = createSelector(selectActiveQuery, selectSearchExecutionResult, (queryId, state) => state?.result?.forId(queryId));
