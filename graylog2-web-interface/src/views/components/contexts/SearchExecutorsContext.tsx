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
import { cancelJob, executeJobResult, startJob } from 'views/logic/slices/executeJobResult';
import parseSearch from 'views/logic/slices/parseSearch';
import { wrapWithOnError } from 'util/conditional/onError';

const defaultStartJob = wrapWithOnError(startJob, 'Starting search job failed', 'Error!');

const defaultExecuteJobResult = wrapWithOnError(executeJobResult, 'Executing search failed', 'Error!');

const defaultSearchExecutors: SearchExecutors = {
  resultMapper: (r) => r,
  parse: parseSearch,
  startJob: defaultStartJob,
  executeJobResult: defaultExecuteJobResult,
  cancelJob,
};
const SearchExecutorsContext = React.createContext<SearchExecutors>(defaultSearchExecutors);
export default SearchExecutorsContext;
