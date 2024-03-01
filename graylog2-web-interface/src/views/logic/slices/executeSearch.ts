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
import type SearchExecutionState from 'views/logic/search/SearchExecutionState';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import type { SearchJobType, JobIds } from 'views/stores/SearchJobs';
import { runSearchJob, searchJobStatus, runStartJob, runPollJob, runCancelJob } from 'views/stores/SearchJobs';
import type View from 'views/logic/views/View';
import type { SearchExecutionResult } from 'views/types';
import SearchResult from 'views/logic/SearchResult';

const delay = (ms: number) => new Promise((resolve) => {
  setTimeout(resolve, ms);
});

const trackJobStatus = (job: SearchJobType): Promise<SearchJobType> => new Promise((resolve) => {
  if (job?.execution?.done || job?.execution?.completed_exceptionally) {
    resolve(job);
  } else {
    resolve(delay(250)
      .then(() => searchJobStatus(job.id))
      .then((jobStatus) => trackJobStatus(jobStatus)));
  }
});

export const startJob = async (
  view: View,
  widgetsToSearch: string[],
  executionStateParam: SearchExecutionState,
  keepQueries: string[] = [],
): Promise<JobIds> => {
  const { widgetMapping, search } = view;

  const globalOverride = (executionStateParam.globalOverride ?? GlobalOverride.empty()).toBuilder()
    .keepQueries(keepQueries)
    .build();

  let executionStateBuilder = executionStateParam.toBuilder().globalOverride(globalOverride);

  if (widgetsToSearch) {
    const keepSearchTypes = widgetsToSearch
      .map((widgetId) => widgetMapping.get(widgetId))
      .reduce((acc, searchTypeSet) => (searchTypeSet ? [...acc, ...searchTypeSet.toArray()] : acc), globalOverride.keepSearchTypes || []);
    const newGlobalOverride = globalOverride.toBuilder().keepSearchTypes(keepSearchTypes).build();
    executionStateBuilder = executionStateBuilder.globalOverride(newGlobalOverride);
  }

  const executionState = executionStateBuilder.build();

  return runStartJob(search, executionState).then((res) => ({ asyncSearchId: res.async_search_id, nodeId: res.node_id }));
};

export const pollJob = (jobIds: JobIds, result: SearchJobType | null, signal: AbortSignal): Promise<SearchJobType> => new Promise((resolve, reject) => {
  if (result) {
    resolve(result);
  } else {
    delay(2000)
      .then(() => {
        if (!signal.aborted) {
          resolve(runPollJob(jobIds).then((res) => pollJob(jobIds, res, signal)));
        } else { reject(); }
      });
  }
});

export const executeJobResult = async ({ asyncSearchId, nodeId }: JobIds, view: View, signal: AbortSignal): Promise<SearchExecutionResult> => {
  const { widgetMapping } = view;

  /*
  if (signal?.aborted) {
    console.log('Aborted');

    return Promise.reject();
  }

  signal.addEventListener('abort', () => {
    console.log('Aborted Listener', { 'signal?.aborted': signal?.aborted });
    Promise.reject();
  });
*/
  return pollJob({ asyncSearchId, nodeId }, null, signal)
    .then((result) => ({ widgetMapping, result: new SearchResult(result) }));
};

export const cancelJob = (jobIds: JobIds) => runCancelJob(jobIds);

const executeSearch = (
  view: View,
  widgetsToSearch: string[],
  executionStateParam: SearchExecutionState,
  keepQueries: string[] = [],
): Promise<SearchExecutionResult> => {
  const { widgetMapping, search } = view;

  const globalOverride = (executionStateParam.globalOverride ?? GlobalOverride.empty()).toBuilder()
    .keepQueries(keepQueries)
    .build();

  let executionStateBuilder = executionStateParam.toBuilder().globalOverride(globalOverride);

  if (widgetsToSearch) {
    const keepSearchTypes = widgetsToSearch
      .map((widgetId) => widgetMapping.get(widgetId))
      .reduce((acc, searchTypeSet) => (searchTypeSet ? [...acc, ...searchTypeSet.toArray()] : acc), globalOverride.keepSearchTypes || []);
    const newGlobalOverride = globalOverride.toBuilder().keepSearchTypes(keepSearchTypes).build();
    executionStateBuilder = executionStateBuilder.globalOverride(newGlobalOverride);
  }

  const executionState = executionStateBuilder.build();

  return runSearchJob(search, executionState)
    .then((job) => trackJobStatus(job))
    .then((result) => ({ widgetMapping, result: new SearchResult(result) }));
};

export default executeSearch;
