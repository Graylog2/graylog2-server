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
import type { AppDispatch } from 'src/stores/useAppDispatch';

import type { ViewState, RootState } from 'views/types';
import type { QueryId } from 'views/logic/queries/Query';
import type ViewStateType from 'views/logic/views/ViewState';
import NewQueryActionHandler from 'views/logic/NewQueryActionHandler';
import type Query from 'views/logic/queries/Query';
import FindNewActiveQueryId from 'views/logic/views/FindNewActiveQuery';
import View from 'views/logic/views/View';
import { setGlobalOverrideQuery, execute } from 'views/logic/slices/searchExecutionSlice';
import isEqualForSearch from 'views/stores/isEqualForSearch';
import UpdateSearchForWidgets from 'views/logic/views/UpdateSearchForWidgets';
import { selectActiveQuery, selectView, selectViewType, selectQueryById } from 'views/logic/slices/viewSelectors';
import createSearch from 'views/logic/slices/createSearch';

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
  const oldView = selectView(getState());

  const oldWidgets = oldView?.state?.map((s) => s.widgets);
  const newWidgets = newView?.state?.map((s) => s.widgets);

  if (recreateSearch || !isEqualForSearch(oldWidgets, newWidgets)) {
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

  return dispatch(loadView(newView, true)).then(() => dispatch(selectQuery(query.id)));
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

  dispatch(loadView(newView, true));
  dispatch(selectQuery(newActiveQuery));
};

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
