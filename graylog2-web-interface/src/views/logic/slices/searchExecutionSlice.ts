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
import {
  selectActiveQuery,
  selectIsDirty,
  selectParameters,
  selectRootView,
  selectView,
} from 'views/logic/slices/viewSelectors';
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
import type { ParameterMap } from 'views/logic/parameters/Parameter';
import type Parameter from 'views/logic/parameters/Parameter';
import type { JobIds, StartJobOptions } from 'views/stores/SearchJobs';
import type Search from 'views/logic/search/Search';
import { setParameters, setView } from 'views/logic/slices/viewSlice';
import createSearch from 'views/logic/slices/createSearch';
import type { WidgetMapping } from 'views/logic/views/types';
import FetchError from 'logic/errors/FetchError';
import ErrorsActions from 'actions/errors/ErrorsActions';
import { createFromFetchError } from 'logic/errors/ReportedErrors';
import UserNotification from 'util/UserNotification';
import { getView } from 'views/api/views';
import ViewTypeLabel from 'views/components/ViewTypeLabel';

const initialState = {
  searchTypesToSearch: undefined,
  executionState: SearchExecutionState.empty(),
  isLoading: false,
  result: undefined,
  jobIds: null,
};

const searchExecutionSlice = createSlice({
  name: 'searchExecution',
  initialState,
  reducers: {
    resetState: () => initialState,
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
  resetState,
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
    options?: StartJobOptions,
  ) => Promise<JobIds>;
  executeJobResult: (params: {
    jobIds: JobIds;
    widgetMapping?: WidgetMapping;
    page?: number;
    perPage?: number;
    stopPolling?: (progress: number) => boolean;
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
    searchExecutors,
    stopPolling,
  }: {
    jobIds: JobIds;
    widgetMapping?: WidgetMapping;
    page?: number;
    perPage?: number;
    searchExecutors: SearchExecutors;
    stopPolling?: (progress: number) => boolean;
  }) =>
  (dispatch: ViewsDispatch, _getState) => {
    dispatch(setJobIds(jobIds));
    dispatch(loading());

    return searchExecutors
      .executeJobResult({ jobIds, widgetMapping, page, perPage, stopPolling })
      .then(searchExecutors.resultMapper)
      .then((result) => {
        dispatch(setJobIds(null));
        const isCanceled = result?.result?.result?.execution?.cancelled;
        if (isCanceled) return dispatch(stopLoading());

        return dispatch(finishedLoading(result));
      });
  };

export const notifyStartJobFailed = (error: unknown) =>
  UserNotification.error(`Starting search job failed: ${error}`, 'Error!');

export const isSearchNotAccessibleError = (error: unknown): error is FetchError =>
  error instanceof FetchError && (error.status === 403 || error.status === 404);

const fetchSavedSearchId = async (viewId: string) => {
  try {
    const { search_id: savedSearchId } = await getView(viewId);

    return savedSearchId;
  } catch {
    return undefined;
  }
};

const isMissingStreamPermission = (error: FetchError) => error.additional?.body?.type === 'MissingStreamPermission';

const SUPERSEDED = 'superseded';

const searchReplacedMeanwhile = (getState: () => RootState, search: Search, widgetMapping: WidgetMapping) => {
  const currentView = selectView(getState());

  if (currentView.search.id === search.id) {
    return undefined;
  }

  return Immutable.is(currentView.widgetMapping, widgetMapping) ? currentView.search : SUPERSEDED;
};

const replaceInaccessibleSearch = async (
  error: FetchError,
  search: Search,
  executedWidgetMapping: WidgetMapping,
  dispatch: ViewsDispatch,
  getState: () => RootState,
): Promise<Search | typeof SUPERSEDED | undefined> => {
  if (isMissingStreamPermission(error)) {
    return undefined;
  }

  const view = selectRootView(getState())?.view;

  if (!view) {
    return undefined;
  }

  const savedSearchId = await fetchSavedSearchId(view.id);

  if (!savedSearchId) {
    return undefined;
  }

  const replacedBeforeCopy = searchReplacedMeanwhile(getState, search, executedWidgetMapping);

  if (replacedBeforeCopy) {
    return replacedBeforeCopy;
  }

  const newSearch = await createSearch(search.toBuilder().newId().build()).catch((copyError) => {
    if (copyError instanceof FetchError && isMissingStreamPermission(copyError)) {
      throw copyError;
    }

    return undefined;
  });

  if (!newSearch) {
    return undefined;
  }

  const replacedWhileCopying = searchReplacedMeanwhile(getState, search, executedWidgetMapping);

  if (replacedWhileCopying) {
    return replacedWhileCopying;
  }

  await dispatch(setView(selectView(getState()).toBuilder().search(newSearch).build()));

  if (savedSearchId !== search.id && (error.status === 403 || !selectIsDirty(getState()))) {
    UserNotification.warning(
      `A newer version of this ${ViewTypeLabel({ type: view.type })} was saved. Reload the page to see it.`,
      'Outdated version',
    );
  }

  return newSearch;
};

const reportSearchNotAccessible = (error: FetchError) => {
  if (error.status === 403) {
    ErrorsActions.report(createFromFetchError(error));
  } else {
    notifyStartJobFailed(error);
  }
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
    stopPolling,
  }: {
    search: Search;
    activeQuery: string;
    searchTypesToSearch: Array<string>;
    executionState: SearchExecutionState;
    searchExecutors: SearchExecutors;
    widgetMapping?: WidgetMapping;
    page?: number;
    perPage?: number;
    stopPolling?: (progress: number) => boolean;
  }) =>
  async (dispatch: ViewsDispatch, getState: () => RootState) => {
    dispatch(loading());
    const executedWidgetMapping = selectRootView(getState())?.view?.widgetMapping;

    return dispatch(parseSearch(search, searchExecutors.parse))
      .then(() => {
        dispatch(cancelExecutedJob());

        return searchExecutors
          .startJob(search, searchTypesToSearch, executionState, [activeQuery], { ignoreUnauthorized: true })
          .catch(async (error) => {
            if (!isSearchNotAccessibleError(error)) {
              throw error;
            }

            const newSearch = await replaceInaccessibleSearch(
              error,
              search,
              executedWidgetMapping,
              dispatch,
              getState,
            ).catch((copyError) => {
              reportSearchNotAccessible(copyError);
              throw copyError;
            });

            if (!newSearch) {
              reportSearchNotAccessible(error);
              throw error;
            }

            if (newSearch === SUPERSEDED || selectActiveQuery(getState()) !== activeQuery) {
              return SUPERSEDED;
            }

            return searchExecutors.startJob(newSearch, searchTypesToSearch, executionState, [activeQuery]);
          });
      })
      .then((jobIds: JobIds | typeof SUPERSEDED) =>
        jobIds === SUPERSEDED
          ? undefined
          : dispatch(executeSearchJob({ searchExecutors, jobIds, widgetMapping, page, perPage, stopPolling })),
      )
      .catch((error) => {
        dispatch(cancelExecutedJob());
        dispatch(stopLoading());

        throw error;
      });
  };

export const execute =
  ({
    search,
    activeQuery,
    widgetMapping,
    page,
    perPage,
    stopPolling,
  }: {
    search: Search;
    activeQuery: string;
    widgetMapping?: WidgetMapping;
    page?: number;
    perPage?: number;
    stopPolling?: (progress: number) => boolean;
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
        stopPolling,
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
