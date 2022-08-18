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
import * as URLUtils from 'util/URLUtils';
import type Search from 'views/logic/search/Search';
import type SearchExecutionState from 'views/logic/search/SearchExecutionState';

const executeQueryUrl = (id) => URLUtils.qualifyUrl(`/views/search/${id}/execute`);
const jobStatusUrl = (jobId) => URLUtils.qualifyUrl(`/views/search/status/${jobId}`);

type SearchJobId = string;
type SearchId = string;

type ExecutionInfoType = {
  done: boolean,
  cancelled: boolean,
  completed_exceptionally: boolean,
};

type SearchJobType = {
  id: SearchJobId,
  search: Search,
  search_id: SearchId,
  results: Map<string, any>,
  execution: ExecutionInfoType,
};

export function runSearchJob(search: Search, executionState: SearchExecutionState): Promise<SearchJobType> {
  return fetch('POST', executeQueryUrl(search.id), JSON.stringify(executionState));
}

export function searchJobStatus(jobId: SearchJobId): Promise<SearchJobType> {
  return fetch('GET', jobStatusUrl(jobId));
}
