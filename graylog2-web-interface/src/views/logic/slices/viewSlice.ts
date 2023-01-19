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
import { createSlice } from '@reduxjs/toolkit';
import type * as Immutable from 'immutable';

import type { AppDispatch } from 'stores/useAppDispatch';
import type { ViewState, RootState, GetState } from 'views/types';
import type { QueryId, TimeRange } from 'views/logic/queries/Query';
import type ViewStateType from 'views/logic/views/ViewState';
import NewQueryActionHandler from 'views/logic/NewQueryActionHandler';
import type Query from 'views/logic/queries/Query';
import FindNewActiveQueryId from 'views/logic/views/FindNewActiveQuery';
import View from 'views/logic/views/View';
import { setGlobalOverrideQuery, execute } from 'views/logic/slices/searchExecutionSlice';
import isEqualForSearch from 'views/stores/isEqualForSearch';
import UpdateSearchForWidgets from 'views/logic/views/UpdateSearchForWidgets';
import {
  selectActiveQuery,
  selectView,
  selectViewType,
  selectQueryById,
  selectSearchQueries, selectViewState, selectSearchQuery,
} from 'views/logic/slices/viewSelectors';
import createSearch from 'views/logic/slices/createSearch';
import type { TitlesMap } from 'views/stores/TitleTypes';
import generateId from 'logic/generateId';

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
    setIsNew: (state: ViewState, action: PayloadAction<boolean>) => ({
      ...state,
      isNew: action.payload,
    }),
    setIsDirty: (state: ViewState, action: PayloadAction<boolean>) => ({
      ...state,
      isDirty: action.payload,
    }),
  },
});
export const viewSliceReducer = viewSlice.reducer;
export const { setView, selectQuery, setIsDirty, setIsNew } = viewSlice.actions;

export const loadView = (newView: View, recreateSearch: boolean = false) => async (dispatch: AppDispatch, getState: () => RootState) => {
  const view = selectView(getState());
  const oldWidgets = view?.state?.map((s) => s.widgets);
  const newWidgets = newView?.state?.map((s) => s.widgets);

  if (recreateSearch || !isEqualForSearch(oldWidgets, newWidgets)) {
    const updatedView = UpdateSearchForWidgets(newView);
    const updatedSearch = await createSearch(updatedView.search);
    const updatedViewWithSearch = updatedView.toBuilder().search(updatedSearch).build();

    await dispatch(setView(updatedViewWithSearch));

    return dispatch(execute());
  }

  return dispatch(setView(newView));
};

export const updateQueries = (newQueries: Immutable.OrderedSet<Query>) => async (dispatch: AppDispatch, getState: () => RootState) => {
  const view = selectView(getState());
  const { search } = view;
  const newView = view.toBuilder()
    .search(search.toBuilder()
      .queries(newQueries)
      .build())
    .build();

  return dispatch(loadView(newView));
};

export const addQuery = (query: Query, viewState: ViewStateType) => async (dispatch: AppDispatch, getState: () => RootState) => {
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

  return dispatch(loadView(newView, true)).then(() => dispatch(selectQuery(query.id)));
};

export const updateQuery = (queryId: QueryId, query: Query) => async (dispatch: AppDispatch, getState: () => RootState) => {
  const state = getState();
  const view = selectView(state);
  const { queries } = view.search;
  const newQueries = queries.map((q) => (q.id === queryId ? query : q)).toOrderedSet();
  const newSearch = view.search.toBuilder()
    .queries(newQueries)
    .build();
  const newView = view.toBuilder()
    .search(newSearch)
    .build();

  return dispatch(loadView(newView, true));
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

  await dispatch(loadView(newView, true));
  await dispatch(selectQuery(newActiveQuery));
};

export const createQuery = () => async (dispatch: AppDispatch, getState: () => RootState) => {
  const viewType = selectViewType(getState());
  const [query, state] = await NewQueryActionHandler(viewType);

  return dispatch(addQuery(query, state));
};

export const duplicateQuery = (queryId: string) => async (dispatch: AppDispatch, getState: GetState) => {
  const newId = generateId();
  const viewState = selectViewState(queryId)(getState());
  const newViewState = viewState.duplicate();

  const searchQuery = selectSearchQuery(queryId)(getState());
  const newSearchQuery = searchQuery.toBuilder().id(newId).build();

  return dispatch(addQuery(newSearchQuery, newViewState));
};

export const setQueriesOrder = (queryIds: Immutable.OrderedSet<string>) => async (dispatch: AppDispatch, getState: () => RootState) => {
  const queries = selectSearchQueries(getState());
  const newQueries = queryIds.map((id) => queries.find((q) => q.id === id)).toOrderedSet();

  return dispatch(updateQueries(newQueries));
};

export const mergeQueryTitles = (newQueryTitles: { queryId: QueryId, titlesMap: TitlesMap}[]) => async (dispatch: AppDispatch, getState: () => RootState) => {
  const view = selectView(getState());
  let newState = view.state;

  newQueryTitles.forEach(({ titlesMap: newQueryTitle, queryId }) => {
    const newViewState = newState.get(queryId);
    let newViewStateTitles = newViewState.titles;

    newQueryTitle.forEach((titles, titleType) => {
      titles.forEach((titleValue, titleID) => {
        newViewStateTitles = newViewStateTitles.setIn([titleType, titleID], titleValue);
      });
    });

    newState = newState.set(queryId, newViewState.toBuilder().titles(newViewStateTitles).build());
  });

  const newView = view.toBuilder()
    .state(newState)
    .build();

  return dispatch(loadView(newView));
};

export const setQueryString = (queryId: QueryId, newQueryString: string) => (dispatch: AppDispatch, getState: () => RootState) => {
  const query = selectQueryById(queryId)(getState());
  const newQuery = query.toBuilder()
    .query({ type: 'elasticsearch', query_string: newQueryString })
    .build();

  return dispatch(updateQuery(queryId, newQuery));
};

export const setTimerange = (queryId: QueryId, timerange: TimeRange) => (dispatch: AppDispatch, getState: () => RootState) => {
  const query = selectQueryById(queryId)(getState());
  const newQuery = query.toBuilder()
    .timerange(timerange)
    .build();

  return dispatch(updateQuery(queryId, newQuery));
};

export const updateQueryString = (queryId: string, newQueryString: string) => (dispatch: AppDispatch, getState: () => RootState) => {
  const viewType = selectViewType(getState());

  if (viewType === View.Type.Search) {
    return dispatch(setQueryString(queryId, newQueryString));
  }

  return dispatch(setGlobalOverrideQuery(newQueryString));
};

export const updateViewState = (id: QueryId, newViewState: ViewStateType) => (dispatch: AppDispatch, getState: () => RootState) => {
  const view = selectView(getState());
  const newState = view.state.set(id, newViewState);
  const newView = view.toBuilder()
    .state(newState)
    .build();

  return dispatch(loadView(newView));
};
