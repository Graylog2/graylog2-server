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
import * as Immutable from 'immutable';

import type { RootState, SearchExecutionResult } from 'views/types';
import { asMock } from 'helpers/mocking';
import { createSearch } from 'fixtures/searches';
import FetchError from 'logic/errors/FetchError';
import ErrorsActions from 'actions/errors/ErrorsActions';
import { createFromFetchError } from 'logic/errors/ReportedErrors';
import UserNotification from 'util/UserNotification';
import { getView } from 'views/api/views';
import SearchMetadata from 'views/logic/search/SearchMetadata';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import type Search from 'views/logic/search/Search';
import type { ViewJson } from 'views/logic/views/View';
import createBackendSearch from 'views/logic/slices/createSearch';
import { setView } from 'views/logic/slices/viewSlice';
import type { SearchExecutors } from 'views/logic/slices/searchExecutionSlice';
import { executeWithExecutionState } from 'views/logic/slices/searchExecutionSlice';

jest.mock('views/api/views', () => ({ getView: jest.fn() }));
jest.mock('views/logic/slices/createSearch', () => jest.fn());
jest.mock('actions/errors/ErrorsActions', () => ({ report: jest.fn() }));
jest.mock('util/UserNotification', () => ({ warning: jest.fn(), error: jest.fn() }));

describe('executeWithExecutionState', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(getView).mockReset();
    asMock(createBackendSearch).mockImplementation(async (search: Search) => search);
  });

  const view = createSearch();

  const jobIds = { asyncSearchId: 'job-id', nodeId: 'node-id' };
  const forbidden = () => new FetchError('Forbidden', 403, undefined);
  const notFound = () => new FetchError('Not Found', 404, undefined);

  const createState = (isDirty = false) =>
    ({
      view: { view, activeQuery: 'query-id-1', isDirty },
      searchExecution: { jobIds: null },
    }) as RootState;

  const execute = ({
    isDirty = false,
    sharedState,
    startJob = jest.fn(() => Promise.reject(forbidden())),
    executeJobResult = jest.fn(async () => ({}) as SearchExecutionResult),
  }: Partial<Pick<SearchExecutors, 'startJob' | 'executeJobResult'>> & {
    isDirty?: boolean;
    sharedState?: RootState;
  } = {}) => {
    const state = sharedState ?? createState(isDirty);
    const searchExecutors: SearchExecutors = {
      resultMapper: (r) => r,
      parse: async () => SearchMetadata.empty(),
      startJob,
      executeJobResult,
      cancelJob: async () => null,
    };
    const dispatch = jest.fn((action) => {
      if (typeof action === 'function') {
        return action(dispatch, () => ({ ...state }), { searchExecutors });
      }

      if (action.type === setView.type) {
        state.view = { ...state.view, view: action.payload[0] };
      }

      return action;
    });

    const result = dispatch(
      executeWithExecutionState({
        search: view.search,
        activeQuery: 'query-id-1',
        searchTypesToSearch: undefined,
        executionState: SearchExecutionState.empty(),
        searchExecutors,
      }),
    );

    return { result, dispatch, state };
  };

  const failingFor = (search: Search, error: FetchError) =>
    jest.fn(async (s: Search) => {
      if (s === search) {
        throw error;
      }

      return jobIds;
    }) as SearchExecutors['startJob'];

  const savedViewWithSearch = (searchId: string) =>
    asMock(getView).mockResolvedValue({ search_id: searchId } as ViewJson);

  it.each([
    ['not accessible', forbidden],
    ['already deleted', notFound],
  ])('continues with a copy of the search when it is %s after another user saved the view', async (_, error) => {
    savedViewWithSearch('newer-search-id');
    const startJob = failingFor(view.search, error());

    const { result, dispatch } = execute({ startJob });
    await result;

    const [newSearch] = asMock(createBackendSearch).mock.calls[0];

    expect(newSearch.id).not.toBe(view.search.id);
    expect(newSearch.queries).toEqual(view.search.queries);
    expect(startJob).toHaveBeenNthCalledWith(1, view.search, undefined, expect.anything(), ['query-id-1'], {
      ignoreUnauthorized: true,
    });
    expect(startJob).toHaveBeenLastCalledWith(newSearch, undefined, expect.anything(), ['query-id-1']);
    expect(dispatch).toHaveBeenCalledWith(setView(view.toBuilder().search(newSearch).build()));
    expect(UserNotification.warning).toHaveBeenCalledWith(
      expect.stringMatching(/newer version .* was saved/),
      'Outdated version',
    );
    expect(ErrorsActions.report).not.toHaveBeenCalled();
  });

  it('retries only once', async () => {
    savedViewWithSearch('newer-search-id');
    const error = forbidden();
    const startJob = jest.fn(() => Promise.reject(error));

    await expect(execute({ startJob }).result).rejects.toBe(error);

    expect(startJob).toHaveBeenCalledTimes(2);
    expect(createBackendSearch).toHaveBeenCalledTimes(1);
  });

  it('continues without an outdated notice when a search of a view with unsaved changes is deleted', async () => {
    savedViewWithSearch('newer-search-id');
    const startJob = failingFor(view.search, notFound());

    await execute({ startJob, isDirty: true }).result;

    expect(createBackendSearch).toHaveBeenCalled();
    expect(UserNotification.warning).not.toHaveBeenCalled();
  });

  it('shows the outdated notice for a view with unsaved changes when its search is not accessible', async () => {
    savedViewWithSearch('newer-search-id');
    const startJob = failingFor(view.search, forbidden());

    await execute({ startJob, isDirty: true }).result;

    expect(UserNotification.warning).toHaveBeenCalledWith(expect.anything(), 'Outdated version');
  });

  it('does not continue when another query became active while the search was copied', async () => {
    savedViewWithSearch('newer-search-id');
    const startJob = failingFor(view.search, forbidden());
    const executeJobResult = jest.fn();
    const execution = execute({ startJob, executeJobResult });
    asMock(createBackendSearch).mockImplementation(async (search: Search) => {
      execution.state.view = { ...execution.state.view, activeQuery: 'query-id-2' };

      return search;
    });

    await execution.result;

    expect(startJob).toHaveBeenCalledTimes(1);
    expect(executeJobResult).not.toHaveBeenCalled();
    expect(ErrorsActions.report).not.toHaveBeenCalled();
  });

  it('continues overlapping executions with the search of the view', async () => {
    savedViewWithSearch('newer-search-id');
    const startJob = failingFor(view.search, forbidden());
    const state = createState();

    await Promise.all([
      execute({ startJob, sharedState: state }).result,
      execute({ startJob, sharedState: state }).result,
    ]);

    const copies = asMock(createBackendSearch).mock.calls.map(([search]) => search);

    expect(copies).toHaveLength(2);
    expect(startJob).toHaveBeenNthCalledWith(3, copies[0], undefined, expect.anything(), ['query-id-1']);
    expect(startJob).toHaveBeenNthCalledWith(4, copies[0], undefined, expect.anything(), ['query-id-1']);
    expect(state.view.view.search).toBe(copies[0]);
    expect(UserNotification.warning).toHaveBeenCalledTimes(1);
    expect(ErrorsActions.report).not.toHaveBeenCalled();
  });

  it('keeps a local edit made while the search is copied and leaves executing it to the edit', async () => {
    savedViewWithSearch('newer-search-id');
    const startJob = failingFor(view.search, forbidden());
    const executeJobResult = jest.fn();
    const editedView = view
      .toBuilder()
      .search(createSearch({ searchId: 'edited-search-id' }).search)
      .state(
        view.state.update('query-id-1', (viewState) =>
          viewState
            .toBuilder()
            .widgetMapping(Immutable.Map({ 'widget-id': Immutable.Set(['new-search-type-id']) }))
            .build(),
        ),
      )
      .build();
    const execution = execute({ startJob, executeJobResult });
    asMock(createBackendSearch).mockImplementation(async (search: Search) => {
      execution.state.view = { ...execution.state.view, view: editedView };

      return search;
    });

    await execution.result;

    expect(execution.state.view.view).toBe(editedView);
    expect(startJob).toHaveBeenCalledTimes(1);
    expect(executeJobResult).not.toHaveBeenCalled();
    expect(ErrorsActions.report).not.toHaveBeenCalled();
    expect(UserNotification.warning).not.toHaveBeenCalled();
  });

  it('leaves executing to a local edit made before the search turned out to be inaccessible', async () => {
    savedViewWithSearch('newer-search-id');
    const executeJobResult = jest.fn();
    const editedView = view
      .toBuilder()
      .search(createSearch({ searchId: 'edited-search-id' }).search)
      .state(
        view.state.update('query-id-1', (viewState) =>
          viewState
            .toBuilder()
            .widgetMapping(Immutable.Map({ 'widget-id': Immutable.Set(['new-search-type-id']) }))
            .build(),
        ),
      )
      .build();
    const sharedState = createState();
    const startJob = jest.fn(async () => {
      sharedState.view = { ...sharedState.view, view: editedView };

      throw forbidden();
    });

    await execute({ startJob, executeJobResult, sharedState }).result;

    expect(sharedState.view.view).toBe(editedView);
    expect(createBackendSearch).not.toHaveBeenCalled();
    expect(startJob).toHaveBeenCalledTimes(1);
    expect(executeJobResult).not.toHaveBeenCalled();
    expect(ErrorsActions.report).not.toHaveBeenCalled();
  });

  it('continues with the search another execution put into the view without copying it again', async () => {
    const copiedView = view
      .toBuilder()
      .search(createSearch({ searchId: 'copied-search-id' }).search)
      .build();
    const startJob = failingFor(view.search, forbidden());
    const execution = execute({ startJob });
    asMock(getView).mockImplementation(async () => {
      execution.state.view = { ...execution.state.view, view: copiedView };

      return { search_id: 'newer-search-id' } as ViewJson;
    });

    await execution.result;

    expect(createBackendSearch).not.toHaveBeenCalled();
    expect(startJob).toHaveBeenLastCalledWith(copiedView.search, undefined, expect.anything(), ['query-id-1']);
    expect(ErrorsActions.report).not.toHaveBeenCalled();
  });

  it('reports the original error when copying the search fails', async () => {
    savedViewWithSearch('newer-search-id');
    asMock(createBackendSearch).mockRejectedValue(new FetchError('Bad Request', 400, undefined));
    const error = forbidden();

    await expect(execute({ startJob: jest.fn(() => Promise.reject(error)) }).result).rejects.toBe(error);

    expect(ErrorsActions.report).toHaveBeenCalledWith(createFromFetchError(error));
  });

  it('reports missing stream permissions of the copy instead of the original error', async () => {
    savedViewWithSearch('newer-search-id');
    const copyError = new FetchError('Forbidden', 403, { type: 'MissingStreamPermission', streams: ['stream-id'] });
    asMock(createBackendSearch).mockRejectedValue(copyError);

    await expect(execute({ startJob: jest.fn(() => Promise.reject(forbidden())) }).result).rejects.toBe(copyError);

    expect(ErrorsActions.report).toHaveBeenCalledTimes(1);
    expect(ErrorsActions.report).toHaveBeenCalledWith(createFromFetchError(copyError));
  });

  it('reports permission error when the view is no longer accessible', async () => {
    asMock(getView).mockRejectedValue(forbidden());
    const error = forbidden();

    await expect(execute({ startJob: jest.fn(() => Promise.reject(error)) }).result).rejects.toBe(error);

    expect(createBackendSearch).not.toHaveBeenCalled();
    expect(ErrorsActions.report).toHaveBeenCalled();
  });

  it('reports the error when there is no view', async () => {
    const error = forbidden();
    const sharedState = { searchExecution: { jobIds: null } } as RootState;

    await expect(execute({ startJob: jest.fn(() => Promise.reject(error)), sharedState }).result).rejects.toBe(error);

    expect(getView).not.toHaveBeenCalled();
    expect(ErrorsActions.report).toHaveBeenCalledWith(createFromFetchError(error));
  });

  it('reports missing stream permissions', async () => {
    savedViewWithSearch('newer-search-id');
    const error = new FetchError('Forbidden', 403, { type: 'MissingStreamPermission', streams: ['stream-id'] });

    await expect(execute({ startJob: jest.fn(() => Promise.reject(error)) }).result).rejects.toBe(error);

    expect(getView).not.toHaveBeenCalled();
    expect(ErrorsActions.report).toHaveBeenCalled();
  });

  it('does not check view when polling the job fails', async () => {
    const error = notFound();
    const startJob = jest.fn(async () => jobIds);
    const executeJobResult = jest.fn(() => Promise.reject(error));

    await expect(execute({ startJob, executeJobResult }).result).rejects.toBe(error);

    expect(getView).not.toHaveBeenCalled();
  });

  it('does not check view for other errors', async () => {
    const error = new FetchError('Server Error', 500, undefined);

    await expect(execute({ startJob: jest.fn(() => Promise.reject(error)) }).result).rejects.toBe(error);

    expect(getView).not.toHaveBeenCalled();
  });
});
