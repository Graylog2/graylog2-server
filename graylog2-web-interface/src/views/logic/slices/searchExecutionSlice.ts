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
import trim from 'lodash/trim';

import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import type {
  SearchExecution,
  RootState,
  GetState,
  SearchExecutionResult,
  ExtraArguments,
  JobIdsState,
} from 'views/types';
import type { ViewsDispatch } from 'views/stores/useViewsDispatch';
import type { SearchParser } from 'views/logic/slices/searchMetadataSlice';
import { parseSearch } from 'views/logic/slices/searchMetadataSlice';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import { selectParameters } from 'views/logic/slices/viewSelectors';
import {
  selectGlobalOverride,
  selectSearchTypesToSearch,
  selectSearchExecutionState,
  selectJobIds,
  selectParameterBindings,
} from 'views/logic/slices/searchExecutionSelectors';
import type { TimeRange } from 'views/logic/queries/Query';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import ParameterBinding from 'views/logic/parameters/ParameterBinding';
import type Parameter from 'views/logic/parameters/Parameter';
import type { ParameterMap } from 'views/logic/parameters/Parameter';
import type { JobIds } from 'views/stores/SearchJobs';
import type Search from 'views/logic/search/Search';
import { setParameters } from 'views/logic/slices/viewSlice';
import type { WidgetMapping } from 'views/logic/views/types';

const searchExecutionSlice = createSlice({
  name: 'searchExecution',
  initialState: {
    searchTypesToSearch: undefined,
    executionState: SearchExecutionState.empty(),
    isLoading: false,
    result: undefined,
    jobIds: null,
  },
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
    setSearchTypesToSearch: (state, action: PayloadAction<Array<string>>) => ({
      ...state,
      searchTypesToSearch: action.payload,
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
      const newParameterBindings = Immutable.Map<string, any>(
        newParameters
          .filter((parameter) => !!parameter.defaultValue)
          .map((parameter) => [parameter.name, ParameterBinding.forValue(parameter.defaultValue)]),
      );
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

export const {
  loading,
  stopLoading,
  finishedLoading,
  updateGlobalOverride,
  setSearchTypesToSearch,
  setParameterValues,
  setParameterBindings,
  setJobIds,
} = searchExecutionSlice.actions;

export const searchExecutionSliceReducer = searchExecutionSlice.reducer;

export type SearchExecutors = {
  parse: SearchParser;
  resultMapper: (newResult: SearchExecutionResult) => SearchExecutionResult;
  startJob: (
    search: Search,
    searchTypesToSearch: string[],
    executionStateParam: SearchExecutionState,
    keepQueries?: string[],
    page?: number,
    perPage?: number,
  ) => Promise<JobIds>;
  executeJobResult: (params: {
    jobIds: JobIds;
    widgetMapping?: WidgetMapping;
    page?: number;
    perPage?: number;
  }) => Promise<SearchExecutionResult>;
  cancelJob: (jobIds: JobIds) => Promise<null>;
};

export const cancelExecutedJob =
  () =>
  (dispatch: ViewsDispatch, getState: () => RootState, { searchExecutors }: ExtraArguments) => {
    const state = getState();
    const jobIds = selectJobIds(state);

    if (jobIds) {
      dispatch(setJobIds(null));

      return searchExecutors.cancelJob(jobIds);
    }

    return Promise.resolve();
  };

export const executeSearchJob =
  ({
    jobIds,
    widgetMapping,
    page,
    perPage,
  }: {
    jobIds: JobIds;
    widgetMapping?: WidgetMapping;
    page?: number;
    perPage?: number;
  }) =>
  (dispatch: ViewsDispatch, _getState, { searchExecutors }: { searchExecutors: SearchExecutors }) => {
    dispatch(setJobIds(jobIds));
    dispatch(loading());

    return searchExecutors
      .executeJobResult({ jobIds, widgetMapping, page, perPage })
      .then(searchExecutors.resultMapper)
      .then((result) => {
        dispatch(setJobIds(null));
        const isCanceled = result?.result?.result?.execution?.cancelled;
        if (isCanceled) return dispatch(stopLoading());

        return dispatch(finishedLoading(result));
      });
  };

export const executeWithExecutionState =
  ({
    search,
    activeQuery,
    searchTypesToSearch,
    executionState,
    searchExecutors,
    widgetMapping,
    page,
    perPage,
  }: {
    search: Search;
    activeQuery: string;
    searchTypesToSearch: Array<string>;
    executionState: SearchExecutionState;
    searchExecutors: SearchExecutors;
    widgetMapping?: WidgetMapping;
    page?: number;
    perPage?: number;
  }) =>
  (dispatch: ViewsDispatch) =>
    dispatch(parseSearch(search, searchExecutors.parse))
      .then(() => {
        dispatch(loading());
        dispatch(cancelExecutedJob());

        return searchExecutors.startJob(search, searchTypesToSearch, executionState, [activeQuery], page, perPage);
      })
      .then((jobIds: JobIds) => {
        dispatch(setJobIds(jobIds));

        return dispatch(executeSearchJob({ jobIds, widgetMapping, page, perPage }));
      });

export const execute =
  ({
    search,
    activeQuery,
    widgetMapping,
    page,
    perPage,
  }: {
    search: Search;
    activeQuery: string;
    widgetMapping?: WidgetMapping;
    page?: number;
    perPage?: number;
  }) =>
  (dispatch: ViewsDispatch, getState: () => RootState, { searchExecutors }: ExtraArguments) => {
    const state = getState();
    const executionState = selectSearchExecutionState(state);
    const searchTypesToSearch = selectSearchTypesToSearch(state);

    return dispatch(
      executeWithExecutionState({
        search,
        activeQuery,
        searchTypesToSearch,
        executionState,
        searchExecutors,
        widgetMapping,
        page,
        perPage,
      }),
    );
  };

export const setGlobalOverrideQuery =
  (queryString: string) => async (dispatch: ViewsDispatch, getState: () => RootState) => {
    const globalOverride = selectGlobalOverride(getState()) ?? GlobalOverride.empty();
    const newGlobalOverride = globalOverride.toBuilder().query(createElasticsearchQueryString(queryString)).build();

    return dispatch(searchExecutionSlice.actions.updateGlobalOverride(newGlobalOverride));
  };

export const setGlobalOverrideTimerange =
  (timerange: TimeRange) => async (dispatch: ViewsDispatch, getState: () => RootState) => {
    const globalOverride = selectGlobalOverride(getState()) ?? GlobalOverride.empty();
    const newGlobalOverride = globalOverride.toBuilder().timerange(timerange).build();

    return dispatch(searchExecutionSlice.actions.updateGlobalOverride(newGlobalOverride));
  };

export const setGlobalOverride =
  (queryString: string, timerange: TimeRange) => (dispatch: ViewsDispatch, getState: () => RootState) => {
    const globalOverride = selectGlobalOverride(getState()) ?? GlobalOverride.empty();
    const newGlobalOverride = globalOverride
      .toBuilder()
      .query(createElasticsearchQueryString(queryString))
      .timerange(timerange)
      .build();

    return dispatch(searchExecutionSlice.actions.updateGlobalOverride(newGlobalOverride));
  };

export const declareParameters =
  (newParameters: ParameterMap) => async (dispatch: ViewsDispatch, getState: GetState) => {
    const parameters = selectParameters(getState()).toArray();
    const newParametersArray = newParameters.valueSeq().toArray();
    await dispatch(searchExecutionSlice.actions.addParameterBindings(newParametersArray));

    return dispatch(setParameters([...parameters, ...newParametersArray]));
  };

export const removeParameter = (parameterName: string) => async (dispatch: ViewsDispatch, getState: GetState) => {
  const parameters = selectParameters(getState());
  const newParameters = parameters.filter((p) => p.name !== parameterName).toArray();
  const parameterBindings = selectParameterBindings(getState());
  const newParameterBindings = parameterBindings.remove(parameterName);

  await dispatch(setParameters(newParameters));

  return dispatch(searchExecutionSlice.actions.setParameterBindings(newParameterBindings));
};

export const updateParameter =
  (parameterName: string, newParameter: Parameter) => async (dispatch: ViewsDispatch, getState: GetState) => {
    const parameters = selectParameters(getState());
    const newParameters = parameters.map((p) => (p.name === parameterName ? newParameter : p)).toArray();
    const parameterBindings = selectParameterBindings(getState());

    if (!trim(parameterBindings.get(parameterName, ParameterBinding.empty()).value) && newParameter.defaultValue) {
      const newParameterBindings = parameterBindings.set(
        parameterName,
        ParameterBinding.forValue(newParameter.defaultValue),
      );
      await dispatch(searchExecutionSlice.actions.setParameterBindings(newParameterBindings));
    }

    return dispatch(setParameters(newParameters));
  };
