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
import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';
import type * as Immutable from 'immutable';

import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import type { SearchExecution, RootState } from 'views/types';
import type { AppDispatch } from 'stores/useAppDispatch';
import { parseSearch } from 'views/logic/slices/searchMetadataSlice';
import executeSearch from 'views/logic/slices/executeSearch';
import type View from 'views/logic/views/View';
import type { SearchExecutionResult } from 'views/actions/SearchActions';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import { selectView } from 'views/logic/slices/viewSelectors';
import {
  selectGlobalOverride,
  selectWidgetsToSearch,
  selectSearchExecutionState,
} from 'views/logic/slices/searchExecutionSelectors';
import type { TimeRange } from 'views/logic/queries/Query';
import ParameterBinding from 'views/logic/parameters/ParameterBinding';

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
    updateGlobalOverride: (state, action: PayloadAction<GlobalOverride>) => ({
      ...state,
      executionState: state.executionState.toBuilder().globalOverride(action.payload).build(),
    }),
    setWidgetsToSearch: (state, action: PayloadAction<Array<string>>) => ({
      ...state,
      widgetsToSearch: action.payload,
    }),
    setParameterValues: (state, action: PayloadAction<Immutable.Map<string, any>>) => {
      const parameterMap = action.payload;
      let { parameterBindings } = state.executionState;

      parameterMap.forEach((value, parameterName) => {
        parameterBindings = parameterBindings.set(parameterName, ParameterBinding.forValue(value));
      });

      return {
        ...state,
        executionState: state.executionState.toBuilder().parameterBindings(parameterBindings).build(),
      };
    },
  },
});

export const { loading, finishedLoading, updateGlobalOverride, setWidgetsToSearch, setParameterValues } = searchExecutionSlice.actions;

export const searchExecutionSliceReducer = searchExecutionSlice.reducer;

export const executeWithExecutionState = (
  view: View, widgetsToSearch: Array<string>, executionState: SearchExecutionState, resultMapper: (newResult: SearchExecutionResult) => SearchExecutionResult,
) => (dispatch: AppDispatch) => dispatch(parseSearch(view.search))
  .then(() => {
    dispatch(loading());

    return executeSearch(view, widgetsToSearch, executionState)
      .then(resultMapper)
      .then((result) => dispatch(finishedLoading(result)));
  });

export const execute = () => (dispatch: AppDispatch, getState: () => RootState) => {
  const state = getState();
  const view = selectView(state);
  const executionState = selectSearchExecutionState(state);
  const widgetsToSearch = selectWidgetsToSearch(state);

  return dispatch(executeWithExecutionState(view, widgetsToSearch, executionState, (result) => result));
};

export const setGlobalOverrideQuery = (queryString: string) => async (dispatch: AppDispatch, getState: () => RootState) => {
  const globalOverride = selectGlobalOverride(getState()) ?? GlobalOverride.empty();
  const newGlobalOverride = globalOverride.toBuilder().query({ type: 'elasticsearch', query_string: queryString }).build();

  return dispatch(searchExecutionSlice.actions.updateGlobalOverride(newGlobalOverride));
};

export const setGlobalOverrideTimerange = (timerange: TimeRange) => async (dispatch: AppDispatch, getState: () => RootState) => {
  const globalOverride = selectGlobalOverride(getState()) ?? GlobalOverride.empty();
  const newGlobalOverride = globalOverride.toBuilder().timerange(timerange).build();

  return dispatch(searchExecutionSlice.actions.updateGlobalOverride(newGlobalOverride));
};

export const setGlobalOverride = (queryString: string, timerange: TimeRange) => (dispatch: AppDispatch, getState: () => RootState) => {
  const globalOverride = selectGlobalOverride(getState()) ?? GlobalOverride.empty();
  const newGlobalOverride = globalOverride.toBuilder()
    .query({ type: 'elasticsearch', query_string: queryString })
    .timerange(timerange)
    .build();

  return dispatch(searchExecutionSlice.actions.updateGlobalOverride(newGlobalOverride));
};
