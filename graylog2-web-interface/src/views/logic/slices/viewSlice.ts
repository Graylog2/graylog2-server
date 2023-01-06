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

import type { ViewState } from 'views/types';
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

export const createQuery = () => async (dispatch: AppDispatch) => {
  const [query, state] = await NewQueryActionHandler();
  dispatch(viewSlice.actions.addQuery([query, state]));
};

export const { selectQuery, removeQuery } = viewSlice.actions;
export const viewSliceReducer = viewSlice.reducer;
