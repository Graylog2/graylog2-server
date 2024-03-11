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
import fetch, {fetchWithSignal} from 'logic/rest/FetchProvider';
import * as URLUtils from 'util/URLUtils';
import type Search from 'views/logic/search/Search';
import type SearchExecutionState from 'views/logic/search/SearchExecutionState';
import type { SearchErrorResponse } from 'views/logic/SearchError';

const executeQueryUrl = (id: string) => URLUtils.qualifyUrl(`/views/search/${id}/execute`);
const jobStatusUrl = (jobId: string) => URLUtils.qualifyUrl(`/views/search/status/${jobId}`);

type SearchJobId = string;
type SearchId = string;

const startJobUrl = (id: string) => URLUtils.qualifyUrl(`/views/search/${id}/execute`);

const cancelJobUrl = (nodeId: string, jobId: string) => URLUtils.qualifyUrl(`/views/searchjobs/${nodeId}/${jobId}/cancel`);

const pollJobUrl = (nodeId: string, jobId: string) => URLUtils.qualifyUrl(`/views/searchjobs/${nodeId}/${jobId}/status`);

type ExecutionInfoType = {
  done: boolean,
  cancelled: boolean,
  completed_exceptionally: boolean,
};

export type SearchJobType = {
  id: SearchJobId,
  search: Search,
  search_id: SearchId,
  results: { [id: string]: any },
  execution: ExecutionInfoType,
  owner: string,
  errors: Array<SearchErrorResponse>,
};

export type JobIdsJson = {
  id: string,
  executing_node: string,
}

export type JobIds = {
  asyncSearchId: string,
  nodeId: string,
}

export function runStartJob(search: Search, executionState: SearchExecutionState): Promise<JobIdsJson> {
  return fetch('POST', startJobUrl(search.id), JSON.stringify(executionState));
}

export function runSearchJob(search: Search, executionState: SearchExecutionState): Promise<SearchJobType> {
  return fetch('POST', executeQueryUrl(search.id), JSON.stringify(executionState));
}

export function runPollJob({ nodeId, asyncSearchId } : JobIds, signal: AbortSignal): Promise<SearchJobType | null> {
  return fetchWithSignal(signal, 'GET', pollJobUrl(nodeId, asyncSearchId));
}

export function searchJobStatus(jobId: SearchJobId): Promise<SearchJobType> {
  return fetch('GET', jobStatusUrl(jobId));
}


export function runCancelJob({ nodeId, asyncSearchId } : JobIds): Promise<null> {
  return fetch('DELETE', cancelJobUrl(nodeId, asyncSearchId));
}
