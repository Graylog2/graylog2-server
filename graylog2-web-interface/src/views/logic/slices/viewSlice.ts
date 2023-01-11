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
import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice, createSelector } from '@reduxjs/toolkit';
import type { AppDispatch } from 'src/stores/useAppDispatch';

import type { ViewState, RootState } from 'views/types';
import type { QueryId } from 'views/logic/queries/Query';
import type ViewStateType from 'views/logic/views/ViewState';
import NewQueryActionHandler from 'views/logic/NewQueryActionHandler';
import type Query from 'views/logic/queries/Query';
import FindNewActiveQueryId from 'views/logic/views/FindNewActiveQuery';
import View from 'views/logic/views/View';
import { setGlobalOverrideQuery, selectGlobalOverride, execute } from 'views/logic/slices/searchExecutionSlice';
import isEqualForSearch from 'views/stores/isEqualForSearch';
import UpdateSearchForWidgets from 'views/logic/views/UpdateSearchForWidgets';
import Search from 'views/logic/search/Search';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';

const viewSlice = createSlice({
  name: 'view',
  initialState: {
    view: undefined,
    isDirty: false,
    isNew: false,
    activeQuery: undefined,
  },
  reducers: {
    selectQuery: (state: ViewState, action: PayloadAction<QueryId>) => ({
      ...state,
      activeQuery: action.payload,
    }),
    setView: (state: ViewState, action: PayloadAction<View>) => {
      return ({
        ...state,
        view: action.payload,
      });
    },
  },
});

export const { setView, selectQuery } = viewSlice.actions;

const createSearchUrl = qualifyUrl('/views/search');
const createSearch = (search: Search) => fetch('POST', createSearchUrl, JSON.stringify(search))
  .then((response) => Search.fromJSON(response));

const loadView = (newView: View) => async (dispatch: AppDispatch, getState: () => RootState) => {
  const oldView = selectView(getState());

  const oldWidgets = oldView?.state?.map((s) => s.widgets);
  const newWidgets = newView?.state?.map((s) => s.widgets);

  if (!isEqualForSearch(oldWidgets, newWidgets)) {
    const updatedView = UpdateSearchForWidgets(newView);
    const updatedSearch = await createSearch(updatedView.search);
    const updatedViewWithSearch = updatedView.toBuilder().search(updatedSearch).build();

    dispatch(setView(updatedViewWithSearch));
    dispatch(execute());
  }

  return dispatch(setView(newView));
};

export const addQuery = (payload: [Query, ViewStateType]) => async (dispatch: AppDispatch, getState: () => RootState) => {
  const [query, viewState] = payload;
  const state = getState();
  const view = selectView(state);
  const { search } = view;
  const newQueries = search.queries.add(query);
  const newViewStates = view.state.set(query.id, viewState);
  const newView = view.toBuilder()
    .search(search.toBuilder()
      .queries(newQueries)
      .build())
    .state(newViewStates)
    .build();

  dispatch(loadView(newView));
  dispatch(selectQuery(query.id));
};

export const updateQuery = (payload: [string, Query]) => async (dispatch: AppDispatch, getState: () => RootState) => {
  const state = getState();
  const view = selectView(state);
  const { queries } = view.search;
  const [queryId, query] = payload;
  const newQueries = queries.map((q) => (q.id === queryId ? query : q)).toOrderedSet();
  const newSearch = view.search.toBuilder()
    .queries(newQueries)
    .build();
  const newView = view.toBuilder()
    .search(newSearch)
    .build();

  return dispatch(loadView(newView));
};

export const removeQuery = (queryId: string) => async (dispatch: AppDispatch, getState: () => RootState) => {
  const state = getState();
  const view = selectView(state);
  const activeQuery = selectActiveQuery(state);
  const { search } = view;
  const newQueries = search.queries.filter((query) => query.id !== queryId).toOrderedSet();
  const newViewState = view.state.remove(queryId);
  const newView = view.toBuilder()
    .search(search.toBuilder().queries(newQueries).build())
    .state(newViewState)
    .build();

  const indexedQueryIds = search.queries.map((query) => query.id).toList();
  const newActiveQuery = FindNewActiveQueryId(indexedQueryIds, activeQuery);

  dispatch(loadView(newView));
  dispatch(selectQuery(newActiveQuery));
};

export const viewSliceReducer = viewSlice.reducer;

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

export const createQuery = () => async (dispatch: AppDispatch, getState: () => RootState) => {
  const viewType = selectViewType(getState());
  const [query, state] = await NewQueryActionHandler(viewType);
  dispatch(addQuery([query, state]));
};

export const setQueryString = (queryId: QueryId, newQueryString: string) => (dispatch: AppDispatch, getState: () => RootState) => {
  const query = selectQueryById(queryId)(getState());
  const newQuery = query.toBuilder()
    .query({ type: 'elasticsearch', query_string: newQueryString })
    .build();

  return dispatch(updateQuery([queryId, newQuery]));
};

export const updateQueryString = (queryId: string, newQueryString: string) => (dispatch: AppDispatch, getState: () => RootState) => {
  const viewType = selectViewType(getState());

  if (viewType === View.Type.Search) {
    return dispatch(setQueryString(queryId, newQueryString));
  }

  return dispatch(setGlobalOverrideQuery(newQueryString));
};
