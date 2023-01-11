import { createSelector } from '@reduxjs/toolkit';

import type { RootState } from 'views/types';
import { selectGlobalOverride } from 'views/logic/slices/searchExecutionSlice';
import View from 'views/logic/views/View';

export const selectRootView = (state: RootState) => state.view;
export const selectView = createSelector(selectRootView, (state) => state.view);
export const selectActiveQuery = createSelector(selectRootView, (state) => state.activeQuery);
export const selectIsDirty = createSelector(selectRootView, (state) => state.isDirty);
export const selectIsNew = createSelector(selectRootView, (state) => state.isNew);
export const selectViewType = createSelector(selectView, (view) => view.type);
export const selectViewStates = createSelector(selectView, (state) => state.state);
export const selectActiveViewState = createSelector(
  selectActiveQuery,
  selectViewStates,
  (activeQuery, states) => states?.get(activeQuery),
);
export const selectSearch = createSelector(selectView, (view) => view.search);
export const selectSearchQueries = createSelector(selectSearch, (search) => search.queries);
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
export const selectCurrentQueryString = (queryId: string) => createSelector(
  selectViewType,
  selectGlobalOverride,
  selectSearch,
  (viewType, globalOverride, search) => (viewType === View.Type.Search
    ? search.queries.find((q) => q.id === queryId).query.query_string
    : globalOverride.query.query_string),
);
