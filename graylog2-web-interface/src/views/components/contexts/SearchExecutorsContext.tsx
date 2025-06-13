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
import * as React from 'react';

import type { SearchExecutors } from 'views/logic/slices/searchExecutionSlice';
import type { ExecuteJobResultType, StartJobType } from 'views/logic/slices/executeJobResult';
import { cancelJob, executeJobResult, startJob } from 'views/logic/slices/executeJobResult';
import parseSearch from 'views/logic/slices/parseSearch';
import { defaultOnError } from 'util/conditional/onError';
import type { JobIds } from 'views/stores/SearchJobs';
import type { SearchExecutionResult } from 'views/types';

const defaultStartJob: StartJobType = (search, searchTypesToSearch, executionStateParam, keepQueries = []) =>
  defaultOnError<JobIds>(
    startJob(search, searchTypesToSearch, executionStateParam, keepQueries),
    'Starting of search job failed',
    'Error!',
  );

const defaultExecuteJobResult: ExecuteJobResultType = (props) =>
  defaultOnError<SearchExecutionResult>(executeJobResult(props), 'Executing of search failed', 'Error!');

const defaultSearchExecutors: SearchExecutors = {
  resultMapper: (r) => r,
  parse: parseSearch,
  startJob: defaultStartJob,
  executeJobResult: defaultExecuteJobResult,
  cancelJob,
};
const SearchExecutorsContext = React.createContext<SearchExecutors>(defaultSearchExecutors);
export default SearchExecutorsContext;
