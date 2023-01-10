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
import { createSlice, createSelector } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import type { SearchExecution, RootState } from 'views/types';
import type { AppDispatch } from 'stores/useAppDispatch';
import { selectView } from 'views/logic/slices/viewSlice';
import { parseSearch } from 'views/logic/slices/searchMetadataSlice';
import executeSearch from 'views/logic/slices/executeSearch';

const searchExecutionSlice = createSlice({
  name: 'searchExecution',
  initialState: {
    widgetsToSearch: undefined,
    executionState: SearchExecutionState.empty(),
    isLoading: false,
    result: undefined,
  } as SearchExecution,
  reducers: {
    loading: (state) => ({
      ...state,
      isLoading: true,
    }),
    finishedLoading: (state, action: PayloadAction<SearchExecution['result']>) => ({
      ...state,
      isLoading: false,
      result: action.payload,
    }),
  },
});

export const { loading, finishedLoading } = searchExecutionSlice.actions;

export const searchExecutionSliceReducer = searchExecutionSlice.reducer;

export const selectSearchExecutionRoot = (state: RootState) => state.searchExecution;

export const selectSearchExecutionState = createSelector(selectSearchExecutionRoot, (state) => state.executionState);
export const selectWidgetsToSearch = createSelector(selectSearchExecutionRoot, (state) => state.widgetsToSearch);
export const selectSearchExecutionResult = createSelector(selectSearchExecutionRoot, (state) => state.result);
export const selectGlobalOverride = createSelector(selectSearchExecutionState, (executionState) => executionState.globalOverride);

export const execute = () => (dispatch: AppDispatch, getState: () => RootState) => {
  const state = getState();
  const view = selectView(state);
  const executionState = selectSearchExecutionState(state);
  const widgetsToSearch = selectWidgetsToSearch(state);

  return dispatch(parseSearch(view.search)).then(() => {
    dispatch(loading());

    return executeSearch(view, widgetsToSearch, executionState).then((result) => dispatch(finishedLoading(result)));
  });
};
