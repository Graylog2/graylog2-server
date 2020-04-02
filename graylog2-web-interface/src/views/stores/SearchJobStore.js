// @flow strict
import Reflux from 'reflux';

import fetch, { redirectIfForbidden } from 'logic/rest/FetchProvider';
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
      const handleForbidden = (error, SessionStore) => {
        if (!error.type === 'MissingStreamPermission') {
          redirectIfForbidden(error, SessionStore);
        }
      };
      const promise = fetch('POST', executeQueryUrl(search.id), JSON.stringify(executionState), handleForbidden);
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
