// @flow strict
import Reflux from 'reflux';

// $FlowFixMe: imports from core need to be fixed in flow
import fetch from 'logic/rest/FetchProvider';
// $FlowFixMe: imports from core need to be fixed in flow
import URLUtils from 'util/URLUtils';

import Search from 'views/logic/search/Search';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';

const executeQueryUrl = id => URLUtils.qualifyUrl(`/views/search/${id}/execute`);
const jobStatusUrl = jobId => URLUtils.qualifyUrl(`/views/search/status/${jobId}`);

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

type SearchJobActionsType = {
  run: (Search, SearchExecutionState) => Promise<SearchJobType>,
  jobStatus: (SearchJobId) => Promise<SearchJobType>,
};

export const SearchJobActions: SearchJobActionsType = Reflux.createActions({
  create: { asyncResult: true },
  run: { asyncResult: true },
  jobStatus: { asyncResult: true },
  remove: { asyncResult: true },
});

export const SearchJobStore = Reflux.createStore({
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

});
