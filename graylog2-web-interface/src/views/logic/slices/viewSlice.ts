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
    addQuery: (state: ViewState, action: PayloadAction<[Query, ViewStateType]>) => {
      const [query, viewState] = action.payload;
      const { view } = state ?? {};
      const { search } = view;
      const newQueries = search.queries.add(query);
      const newViewStates = view.state.set(query.id, viewState);
      const newView = view.toBuilder()
        .search(search.toBuilder()
          .queries(newQueries)
          .build())
        .state(newViewStates)
        .build();

      return {
        ...state,
        view: newView,
        activeQuery: query.id,
      };
    },
    removeQuery: (state: ViewState, action: PayloadAction<string>) => {
      const queryId = action.payload;
      const { view, activeQuery } = state ?? {};
      const { search } = view;
      const newQueries = search.queries.filter((query) => query.id !== queryId).toOrderedSet();
      const newViewState = view.state.remove(queryId);
      const newView = view.toBuilder()
        .search(search.toBuilder().queries(newQueries).build())
        .state(newViewState)
        .build();

      const indexedQueryIds = search.queries.map((query) => query.id).toList();
      const newActiveQuery = FindNewActiveQueryId(indexedQueryIds, activeQuery);

      return {
        ...state,
        view: newView,
        activeQuery: newActiveQuery,
      };
    },
  },
});

export const { selectQuery, removeQuery } = viewSlice.actions;
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

export const selectWidgets = createSelector(selectActiveViewState, (viewState) => viewState.widgets);
export const selectWidget = (widgetId: string) => createSelector(selectWidgets, (widgets) => widgets.find((widget) => widget.id === widgetId));
export const selectTitles = createSelector(selectActiveViewState, (viewState) => viewState.titles);

export const createQuery = () => async (dispatch: AppDispatch, getState: () => RootState) => {
  const viewType = selectViewType(getState());
  const [query, state] = await NewQueryActionHandler(viewType);
  dispatch(viewSlice.actions.addQuery([query, state]));
};
