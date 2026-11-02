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
import generateId from 'logic/generateId';
import { qualifyUrl } from 'util/URLUtils';
import { wrapWithOnError } from 'util/conditional/onError';
import SearchResult from 'views/logic/SearchResult';
import type Search from 'views/logic/search/Search';
import type SearchExecutionState from 'views/logic/search/SearchExecutionState';
import type { SearchJobType } from 'views/stores/SearchJobs';
import type { SearchExecutors } from 'views/logic/slices/searchExecutionSlice';
import { buildSearchExecutionState } from 'views/logic/slices/executeJobResult';
import parseSearch from 'views/logic/slices/parseSearch';

const SYNC_NODE_ID = 'sync';

// Results of searches that already ran in `startJob`, until `executeJobResult` picks them up.
const finishedJobs = new Map<string, SearchJobType>();

const startJob = async (
  search: Search,
  searchTypesToSearch: Array<string>,
  executionStateParam: SearchExecutionState,
  keepQueries: Array<string> = [],
) => {
  const executionState = buildSearchExecutionState(searchTypesToSearch, executionStateParam, keepQueries);
  const result: SearchJobType = await fetch('POST', qualifyUrl('/views/search/sync/execute'), {
    search: search.toJSON(),
    execution_state: executionState.toJSON(),
  });
  const asyncSearchId = generateId();
  finishedJobs.set(asyncSearchId, result);

  return { asyncSearchId, nodeId: SYNC_NODE_ID };
};

const executeJobResult: SearchExecutors['executeJobResult'] = async ({ jobIds, widgetMapping }) => {
  const result = finishedJobs.get(jobIds.asyncSearchId);
  finishedJobs.delete(jobIds.asyncSearchId);

  return { result: new SearchResult(result), widgetMapping };
};

/**
 * Search executors that run searches without storing them, through `POST /views/search/sync/execute`.
 * Searches are executed synchronously in `startJob`, so they cannot be cancelled.
 */
const syncSearchExecutors: SearchExecutors = {
  parse: parseSearch,
  resultMapper: (result) => result,
  startJob: wrapWithOnError(startJob, 'Executing search failed', 'Error!'),
  executeJobResult,
  cancelJob: () => Promise.resolve(null),
  createSearch: (search: Search) => Promise.resolve(search),
};

export default syncSearchExecutors;
