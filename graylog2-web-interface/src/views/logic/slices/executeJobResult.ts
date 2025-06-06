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
import { runStartJob, runPollJob, runCancelJob } from 'views/stores/SearchJobs';
import type { SearchExecutionResult } from 'views/types';
import SearchResult from 'views/logic/SearchResult';
import type Search from 'views/logic/search/Search';
import type { WidgetMapping } from 'views/logic/views/types';

const delay = (ms: number) =>
  new Promise((resolve) => {
    setTimeout(resolve, ms);
  });

export const buildSearchExecutionState = (
  searchTypesToSearch: string[],
  executionStateParam: SearchExecutionState,
  keepQueries: string[] = [],
): SearchExecutionState => {
  const globalOverride = (executionStateParam.globalOverride ?? GlobalOverride.empty())
    .toBuilder()
    .keepQueries(keepQueries)
    .build();

  let executionStateBuilder = executionStateParam.toBuilder().globalOverride(globalOverride);

  if (searchTypesToSearch) {
    const keepSearchTypes = [...(globalOverride.keepSearchTypes || []), ...searchTypesToSearch];
    const newGlobalOverride = globalOverride.toBuilder().keepSearchTypes(keepSearchTypes).build();
    executionStateBuilder = executionStateBuilder.globalOverride(newGlobalOverride);
  }

  return executionStateBuilder.build();
};

export const startJob = async (
  search: Search,
  searchTypesToSearch: string[],
  executionStateParam: SearchExecutionState,
  keepQueries: string[] = [],
): Promise<JobIds> => {
  const executionState = buildSearchExecutionState(searchTypesToSearch, executionStateParam, keepQueries);

  return runStartJob(search, executionState).then((res) => ({ asyncSearchId: res.id, nodeId: res.executing_node }));
};

const getDelayTime = (depth: number = 1): number => {
  // increase the delay time by 250ms after every 10th usage but not more than 2500ms
  const curDepth = Math.min(Math.max(1, depth), 100);

  return Math.ceil(curDepth / 10) * 250;
};

export const pollJob = ({
  jobIds,
  result,
  depth = 1,
  page,
  perPage,
  stopPolling,
}: {
  jobIds: JobIds;
  result: SearchJobType | null;
  depth?: number;
  page?: number;
  perPage?: number;
  stopPolling?: (progress: number) => boolean;
}): Promise<SearchJobType> =>
  new Promise((resolve) => {
    if (stopPolling?.(result?.progress ?? 0) || result?.execution?.done || result?.execution?.cancelled) {
      resolve(result);
    } else {
      delay(getDelayTime(depth)).then(() => {
        resolve(
          runPollJob({ jobIds, page, perPage }).then((res) =>
            pollJob({ jobIds, result: res, depth: depth + 1, page, perPage, stopPolling }),
          ),
        );
      });
    }
  });

export const executeJobResult = async ({
  jobIds: { asyncSearchId, nodeId },
  widgetMapping,
  page,
  perPage,
  stopPolling,
}: {
  jobIds: JobIds;
  widgetMapping?: WidgetMapping;
  page?: number;
  perPage?: number;
  stopPolling?: (progress: number) => boolean;
}): Promise<SearchExecutionResult> =>
  pollJob({
    jobIds: { asyncSearchId, nodeId },
    result: null,
    depth: 1,
    page,
    perPage,
    stopPolling,
  }).then((result) => ({
    result: new SearchResult(result),
    widgetMapping,
    jobIds: { asyncSearchId, nodeId },
  }));

export const cancelJob = (jobIds: JobIds) => runCancelJob(jobIds);

export default executeJobResult;
