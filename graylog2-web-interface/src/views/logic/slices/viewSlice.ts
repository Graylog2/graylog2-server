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
import * as Immutable from 'immutable';

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
  selectSearchQueries, selectViewState, selectSearchQuery, selectSearch,
} from 'views/logic/slices/viewSelectors';
import createSearch from 'views/logic/slices/createSearch';
import type { TitlesMap } from 'views/stores/TitleTypes';
import generateId from 'logic/generateId';
import type Parameter from 'views/logic/parameters/Parameter';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import { pushIntoRevisions } from 'views/logic/slices/undoRedoSlice';

const viewSlice = createSlice({
  name: 'view',
  initialState: {
    view: undefined,
    isDirty: false,
    isNew: false,
    activeQuery: undefined,
  },
  reducers: {
    setActiveQuery: (state: ViewState, action: PayloadAction<QueryId>) => ({
      ...state,
      activeQuery: action.payload,
    }),
    setView: {
      reducer(state: ViewState, action: PayloadAction<readonly [View, boolean | undefined]>) {
        const [view, isDirty] = action.payload;

        return ({
          ...state,
          view,
          isDirty: isDirty === undefined ? state.isDirty : isDirty,
        });
      },
      prepare(view: View, isDirty?: boolean) {
        return {
          payload: [view, isDirty] as const,
        };
      },
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
export const { setView, setIsDirty, setIsNew, setActiveQuery } = viewSlice.actions;

export const isViewWidgetsEqualForSearch = (view: View, newView: View) => {
  const oldWidgets = view?.state?.map((s) => s.widgets);
  const newWidgets = newView?.state?.map((s) => s.widgets);

  return isEqualForSearch(oldWidgets, newWidgets);
};

const _recreateSearch = async (newView: View) => {
  const updatedView = UpdateSearchForWidgets(newView);
  const updatedSearch = await createSearch(updatedView.search);

  return updatedView.toBuilder().search(updatedSearch).build();
};

export const selectQuery = (activeQuery: string) => async (dispatch: AppDispatch, getState: () => RootState) => {
  const currentActiveQuery = selectActiveQuery(getState());
  dispatch(setActiveQuery(activeQuery));

  if (currentActiveQuery !== activeQuery) {
    dispatch(execute());
  }
};

export const loadView = (newView: View, recreateSearch: boolean = false) => async (dispatch: AppDispatch, getState: () => RootState) => {
  const view = selectView(getState());

  if (recreateSearch || !isViewWidgetsEqualForSearch(view, newView)) {
    const updatedViewWithSearch = await _recreateSearch(newView);

    await dispatch(setView(updatedViewWithSearch));

    return dispatch(execute());
  }

  return dispatch(setView(newView));
};

type UpdateViewOptions = { hasToPushRevision: boolean };
const defaultUpdateViewOptions = { hasToPushRevision: true };

export const updateView = (
  newView: View,
  recreateSearch: boolean = false,
  options: UpdateViewOptions = defaultUpdateViewOptions,
) => async (dispatch: AppDispatch, getState: () => RootState) => {
  const state = getState();
  const view = selectView(state);

  if (options.hasToPushRevision) {
    await dispatch(pushIntoRevisions({
      type: 'view',
      state: {
        ...state.view,
      },
    }));
  }

  if (recreateSearch || !isViewWidgetsEqualForSearch(view, newView)) {
    const updatedViewWithSearch = await _recreateSearch(newView);
    await dispatch(setView(updatedViewWithSearch, true));

    return dispatch(execute());
  }

  return dispatch(setView(newView, true));
};

export const updateQueries = (newQueries: Immutable.OrderedSet<Query>) => async (dispatch: AppDispatch, getState: () => RootState) => {
  const view = selectView(getState());
  const { search } = view;
  const newSearch = search.toBuilder()
    .newId()
    .queries(newQueries)
    .build();

  const searchAfterSave = await createSearch(newSearch);
  const newViewState = view.state.filter((_state, queryId) => (
    !!newQueries.find((query) => query.id === queryId)),
  ).toMap();

  const newView = view.toBuilder()
    .search(searchAfterSave)
    .state(newViewState)
    .build();

  return dispatch(updateView(newView));
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

  return dispatch(updateView(newView, true)).then(() => dispatch(selectQuery(query.id))).then(() => query.id);
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

  return dispatch(updateView(newView, true));
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
  const newActiveQuery = FindNewActiveQueryId(indexedQueryIds, activeQuery, Immutable.List([queryId]));

  await dispatch(selectQuery(newActiveQuery));
  await dispatch(updateView(newView, true));
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

  await dispatch(addQuery(newSearchQuery, newViewState));

  return newId;
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

  return dispatch(updateView(newView));
};

export const setQueryString = (queryId: QueryId, newQueryString: string) => (dispatch: AppDispatch, getState: () => RootState) => {
  const query = selectQueryById(queryId)(getState());
  const newQuery = query.toBuilder()
    .query(createElasticsearchQueryString(newQueryString))
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

  return dispatch(setGlobalOverrideQuery(newQueryString)).then(() => dispatch(execute()));
};

export const updateViewState = (id: QueryId, newViewState: ViewStateType) => (dispatch: AppDispatch, getState: () => RootState) => {
  const view = selectView(getState());
  const newState = view.state.set(id, newViewState);
  const newView = view.toBuilder()
    .state(newState)
    .build();

  return dispatch(updateView(newView));
};

export const setParameters = (newParameters: Array<Parameter>) => async (dispatch: AppDispatch, getState: () => RootState) => {
  const view = selectView(getState());
  const search = selectSearch(getState());
  const newSearch = search.toBuilder()
    .parameters(newParameters)
    .build();

  const newView = view.toBuilder().search(newSearch).build();

  return dispatch(updateView(newView, true));
};
