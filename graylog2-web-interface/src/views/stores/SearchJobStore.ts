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
// @flow strict
import Reflux from 'reflux';

import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';
import Search from 'views/logic/search/Search';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import type { RefluxActions } from 'stores/StoreTypes';

const executeQueryUrl = (id) => URLUtils.qualifyUrl(`/views/search/${id}/execute`);
const jobStatusUrl = (jobId) => URLUtils.qualifyUrl(`/views/search/status/${jobId}`);

type InternalState = {};

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

type SearchJobActionsType = RefluxActions<{
  run: (Search, SearchExecutionState) => Promise<SearchJobType>,
  jobStatus: (SearchJobId) => Promise<SearchJobType>,
}>;

export const SearchJobActions: SearchJobActionsType = singletonActions(
  'views.SearchJob',
  () => Reflux.createActions({
    create: { asyncResult: true },
    run: { asyncResult: true },
    jobStatus: { asyncResult: true },
    remove: { asyncResult: true },
  }),
);

export const SearchJobStore = singletonStore(
  'views.SearchJob',
  () => Reflux.createStore({
    listenables: [SearchJobActions],

    state: {
      searches: {},
      jobs: {},
    },

    getInitialState(): InternalState {
      return {
        searches: this.state.searches,
        jobs: this.state.jobs,
      };
    },

    run(search: Search, executionState: SearchExecutionState): Promise<SearchJobType> {
      const promise = fetch('POST', executeQueryUrl(search.id), JSON.stringify(executionState));

      SearchJobActions.run.promise(promise);

      return promise;
    },

    jobStatus(jobId: SearchJobId): Promise<SearchJobType> {
      const promise = fetch('GET', jobStatusUrl(jobId));

      SearchJobActions.jobStatus.promise(promise);

      return promise;
    },

  }),
);
