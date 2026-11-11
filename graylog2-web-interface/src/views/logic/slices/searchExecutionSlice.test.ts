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
import type { RootState } from 'views/types';
import { asMock } from 'helpers/mocking';
import { createSearch } from 'fixtures/searches';
import FetchError from 'logic/errors/FetchError';
import ErrorsActions from 'actions/errors/ErrorsActions';
import UserNotification from 'util/UserNotification';
import { getView } from 'views/api/views';
import SearchMetadata from 'views/logic/search/SearchMetadata';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import type { ViewJson } from 'views/logic/views/View';
import type { SearchExecutors } from 'views/logic/slices/searchExecutionSlice';
import { executeWithExecutionState } from 'views/logic/slices/searchExecutionSlice';

jest.mock('views/api/views', () => ({ getView: jest.fn() }));
jest.mock('actions/errors/ErrorsActions', () => ({ report: jest.fn() }));
jest.mock('util/UserNotification', () => ({ warning: jest.fn(), error: jest.fn() }));

describe('executeWithExecutionState', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const view = createSearch();
  const createState = (isDirty = false) =>
    ({ view: { view, activeQuery: 'query-id-1', isDirty }, searchExecution: { jobIds: null } }) as RootState;

  const execute = (
    error: Error,
    {
      state = createState(),
      startJob = jest.fn(() => Promise.reject(error)),
      executeJobResult = jest.fn(),
    }: Partial<Pick<SearchExecutors, 'startJob' | 'executeJobResult'>> & { state?: RootState } = {},
  ) => {
    const searchExecutors: SearchExecutors = {
      resultMapper: (r) => r,
      parse: async () => SearchMetadata.empty(),
      startJob,
      executeJobResult,
      cancelJob: async () => null,
    };
    const dispatch = jest.fn((action) => (typeof action === 'function' ? action(dispatch, () => state, { searchExecutors }) : action));

    return dispatch(
      executeWithExecutionState({
        search: view.search,
        activeQuery: 'query-id-1',
        searchTypesToSearch: undefined,
        executionState: SearchExecutionState.empty(),
        searchExecutors,
      }),
    );
  };

  const savedViewWithSearch = (searchId: string) =>
    asMock(getView).mockResolvedValue({ search_id: searchId } as ViewJson);

  it('shows outdated notice instead of error page when view was saved with another search', async () => {
    savedViewWithSearch('newer-search-id');
    const error = new FetchError('Forbidden', 403, undefined);

    await expect(execute(error)).rejects.toBe(error);

    expect(UserNotification.warning).toHaveBeenCalledWith(expect.stringMatching(/changed by another user/), expect.any(String));
    expect(ErrorsActions.report).not.toHaveBeenCalled();
  });

  it('shows outdated notice when previous search was already deleted', async () => {
    savedViewWithSearch('newer-search-id');

    await expect(execute(new FetchError('Not Found', 404, undefined))).rejects.toBeInstanceOf(FetchError);

    expect(UserNotification.warning).toHaveBeenCalled();
    expect(UserNotification.error).not.toHaveBeenCalled();
  });

  it('reports permission error when search is still current', async () => {
    savedViewWithSearch(view.search.id);

    await expect(execute(new FetchError('Forbidden', 403, undefined))).rejects.toBeInstanceOf(FetchError);

    expect(ErrorsActions.report).toHaveBeenCalled();
    expect(UserNotification.warning).not.toHaveBeenCalled();
  });

  it('starts job without triggering the global permission error page', async () => {
    const startJob = jest.fn(() => Promise.reject(new FetchError('Server Error', 500, undefined)));

    await expect(execute(new FetchError('Server Error', 500, undefined), { startJob })).rejects.toBeInstanceOf(
      FetchError,
    );

    expect(startJob).toHaveBeenCalledWith(view.search, undefined, expect.anything(), ['query-id-1'], {
      ignoreUnauthorized: true,
    });
  });

  it('reports permission error for a view with unsaved changes', async () => {
    savedViewWithSearch('newer-search-id');

    const error = new FetchError('Forbidden', 403, undefined);

    await expect(execute(error, { state: createState(true) })).rejects.toBe(error);

    expect(getView).not.toHaveBeenCalled();
    expect(ErrorsActions.report).toHaveBeenCalled();
  });

  it('reports missing stream permissions', async () => {
    savedViewWithSearch('newer-search-id');
    const error = new FetchError('Forbidden', 403, { type: 'MissingStreamPermission', streams: ['stream-id'] });

    await expect(execute(error)).rejects.toBe(error);

    expect(getView).not.toHaveBeenCalled();
    expect(ErrorsActions.report).toHaveBeenCalled();
  });

  it('does not check view when polling the job fails', async () => {
    const error = new FetchError('Not Found', 404, undefined);
    const startJob = jest.fn(() => Promise.resolve({ asyncSearchId: 'job-id', nodeId: 'node-id' }));
    const executeJobResult = jest.fn(() => Promise.reject(error));

    await expect(execute(error, { startJob, executeJobResult })).rejects.toBe(error);

    expect(getView).not.toHaveBeenCalled();
    expect(UserNotification.error).not.toHaveBeenCalled();
  });

  it('does not check view for other errors', async () => {
    await expect(execute(new FetchError('Server Error', 500, undefined))).rejects.toBeInstanceOf(FetchError);

    expect(getView).not.toHaveBeenCalled();
  });
});
