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
import * as Immutable from 'immutable';
import trim from 'lodash/trim';

import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import type { SearchExecution, RootState, GetState, SearchExecutionResult, ExtraArguments, JobIdsState } from 'views/types';
import type { AppDispatch } from 'stores/useAppDispatch';
import type { SearchParser } from 'views/logic/slices/searchMetadataSlice';
import { parseSearch } from 'views/logic/slices/searchMetadataSlice';
import type View from 'views/logic/views/View';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import { selectView, selectParameters, selectActiveQuery } from 'views/logic/slices/viewSelectors';
import {
  selectGlobalOverride,
  selectWidgetsToSearch,
  selectSearchExecutionState, selectParameterBindings, selectJobIds,
} from 'views/logic/slices/searchExecutionSelectors';
import type { TimeRange } from 'views/logic/queries/Query';
import ParameterBinding from 'views/logic/parameters/ParameterBinding';
import type { ParameterMap } from 'views/logic/parameters/Parameter';
import type Parameter from 'views/logic/parameters/Parameter';
import { setParameters } from 'views/logic/slices/viewSlice';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import type { JobIds } from 'views/stores/SearchJobs';

const searchExecutionSlice = createSlice({
  name: 'searchExecution',
  initialState: {
    widgetsToSearch: undefined,
    executionState: SearchExecutionState.empty(),
    isLoading: false,
    result: undefined,
    jobIds: null,
  } as SearchExecution,
  reducers: {
    loading: (state) => ({
      ...state,
      isLoading: true,
    }),
    stopLoading: (state) => ({
      ...state,
      isLoading: false,
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
    setParameterBindings: (state, action: PayloadAction<Immutable.Map<string, ParameterBinding>>) => ({
      ...state,
      executionState: state.executionState.toBuilder().parameterBindings(action.payload).build(),
    }),
    addParameterBindings: (state, action: PayloadAction<Array<Parameter>>) => {
      const { parameterBindings } = state.executionState;
      const newParameters = action.payload;
      const newParameterBindings = Immutable.Map<string, any>(newParameters.filter((parameter) => !!parameter.defaultValue)
        .map((parameter) => [parameter.name, ParameterBinding.forValue(parameter.defaultValue)]));
      const mergedParameterBindings = parameterBindings.merge(newParameterBindings);

      return {
        ...state,
        executionState: state.executionState.toBuilder().parameterBindings(mergedParameterBindings).build(),
      };
    },
    setJobIds: (state, action: PayloadAction<JobIdsState>) => ({
      ...state,
      jobIds: action.payload,
    }),
  },
});

export const { loading, stopLoading, finishedLoading, updateGlobalOverride, setWidgetsToSearch, setParameterValues, setParameterBindings, setJobIds } = searchExecutionSlice.actions;

export const searchExecutionSliceReducer = searchExecutionSlice.reducer;

export type SearchExecutors = {
  parse: SearchParser,
  resultMapper: (newResult: SearchExecutionResult) => SearchExecutionResult,
  startJob: (view: View, widgetsToSearch: string[], executionStateParam: SearchExecutionState, keepQueries?: string[]) => Promise<JobIds>,
  executeJobResult: (jobIds: JobIds, view: View) => Promise<SearchExecutionResult>,
  cancelJob: (jobIds: JobIds) => Promise<null>,
};

export const cancelExecutedJob = () => (dispatch: AppDispatch, getState: () => RootState, { searchExecutors }: ExtraArguments) => {
  const state = getState();
  const jobIds = selectJobIds(state);

  if (jobIds) {
    dispatch(setJobIds(null));

    searchExecutors.cancelJob(jobIds);
  }
};

export const executeWithExecutionState = (view: View, widgetsToSearch: Array<string>, executionState: SearchExecutionState, searchExecutors: SearchExecutors) => (
  dispatch: AppDispatch,
  getState: GetState,
) => dispatch(parseSearch(view.search, searchExecutors.parse))
  .then(() => {
    dispatch(loading());
    dispatch(cancelExecutedJob());

    const activeQuery = selectActiveQuery(getState());

    return searchExecutors.startJob(view, widgetsToSearch, executionState, [activeQuery]);
  })
  .then((jobIds: JobIds) => {
    dispatch(setJobIds(jobIds));

    return searchExecutors.executeJobResult(jobIds, view)
      .then(searchExecutors.resultMapper)
      .then((result) => {
        dispatch(setJobIds(null));
        const isCanceled = result?.result?.result?.execution?.cancelled;
        if (isCanceled) return dispatch(stopLoading());

        return dispatch(finishedLoading(result));
      });
  });

export const execute = () => (dispatch: AppDispatch, getState: () => RootState, { searchExecutors }: ExtraArguments) => {
  const state = getState();
  const view = selectView(state);
  const executionState = selectSearchExecutionState(state);
  const widgetsToSearch = selectWidgetsToSearch(state);

  return dispatch(executeWithExecutionState(view, widgetsToSearch, executionState, searchExecutors));
};

export const setGlobalOverrideQuery = (queryString: string) => async (dispatch: AppDispatch, getState: () => RootState) => {
  const globalOverride = selectGlobalOverride(getState()) ?? GlobalOverride.empty();
  const newGlobalOverride = globalOverride.toBuilder().query(createElasticsearchQueryString(queryString)).build();

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
    .query(createElasticsearchQueryString(queryString))
    .timerange(timerange)
    .build();

  return dispatch(searchExecutionSlice.actions.updateGlobalOverride(newGlobalOverride));
};

export const declareParameters = (newParameters: ParameterMap) => async (dispatch: AppDispatch, getState: GetState) => {
  const parameters = selectParameters(getState()).toArray();
  const newParametersArray = newParameters.valueSeq().toArray();
  await dispatch(searchExecutionSlice.actions.addParameterBindings(newParametersArray));

  return dispatch(setParameters([...parameters, ...newParametersArray]));
};

export const removeParameter = (parameterName: string) => async (dispatch: AppDispatch, getState: GetState) => {
  const parameters = selectParameters(getState());
  const newParameters = parameters.filter((p) => p.name !== parameterName).toArray();
  const parameterBindings = selectParameterBindings(getState());
  const newParameterBindings = parameterBindings.remove(parameterName);

  await dispatch(setParameters(newParameters));

  return dispatch(searchExecutionSlice.actions.setParameterBindings(newParameterBindings));
};

export const updateParameter = (parameterName: string, newParameter: Parameter) => async (dispatch: AppDispatch, getState: GetState) => {
  const parameters = selectParameters(getState());
  const newParameters = parameters.map((p) => (p.name === parameterName ? newParameter : p)).toArray();
  const parameterBindings = selectParameterBindings(getState());

  if (!trim(parameterBindings.get(parameterName, ParameterBinding.empty()).value) && newParameter.defaultValue) {
    const newParameterBindings = parameterBindings.set(parameterName, ParameterBinding.forValue(newParameter.defaultValue));
    await dispatch(searchExecutionSlice.actions.setParameterBindings(newParameterBindings));
  }

  return dispatch(setParameters(newParameters));
};
