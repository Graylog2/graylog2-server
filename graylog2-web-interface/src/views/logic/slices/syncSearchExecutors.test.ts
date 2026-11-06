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
import fetch from 'logic/rest/FetchProvider';
import { asMock } from 'helpers/mocking';
import Search from 'views/logic/search/Search';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import type { SearchJobType } from 'views/stores/SearchJobs';

import syncSearchExecutors from './syncSearchExecutors';

jest.mock('logic/rest/FetchProvider', () => jest.fn());

const search = Search.create();
const searchJob: SearchJobType = {
  id: 'search-job-id',
  search,
  search_id: search.id,
  progress: 100,
  results: {},
  execution: { done: true, cancelled: false, completed_exceptionally: false },
  owner: 'admin',
  errors: [],
};

describe('syncSearchExecutors', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(fetch).mockResolvedValue(searchJob);
  });

  it('runs the search with its execution state, without storing it', async () => {
    await syncSearchExecutors.startJob(search, ['search-type-1'], SearchExecutionState.empty());

    expect(fetch).toHaveBeenCalledTimes(1);

    const [method, url, requestBody] = asMock(fetch).mock.calls[0];
    // As sent to the backend.
    const body = JSON.parse(JSON.stringify(requestBody));

    expect(method).toBe('POST');
    expect(url).toMatch(/\/views\/search\/sync\/execute$/);
    expect(body.search.id).toBe(search.id);
    expect(body.execution_state.global_override.keep_search_types).toEqual(['search-type-1']);
  });

  it('returns the result of the search it ran', async () => {
    const jobIds = await syncSearchExecutors.startJob(search, [], SearchExecutionState.empty());
    const { result } = await syncSearchExecutors.executeJobResult({ jobIds });

    expect(result.result).toEqual(searchJob);
  });

  it('creates searches without storing them', async () => {
    expect(await syncSearchExecutors.createSearch(search)).toBe(search);
    expect(fetch).not.toHaveBeenCalled();
  });
});
