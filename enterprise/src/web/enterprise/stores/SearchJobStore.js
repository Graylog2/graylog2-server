import Reflux from 'reflux';

import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';
import Search from 'enterprise/logic/search/Search';
import SearchExecutionState from '../logic/search/SearchExecutionState';

const createSearchUrl = URLUtils.qualifyUrl('/plugins/org.graylog.plugins.enterprise/search');
const executeQueryUrl = id => URLUtils.qualifyUrl(`/plugins/org.graylog.plugins.enterprise/search/${id}/execute`);
const jobStatusUrl = jobId => URLUtils.qualifyUrl(`/plugins/org.graylog.plugins.enterprise/search/status/${jobId}`);

export const SearchJobActions: SearchJobActionsType = Reflux.createActions({
  create: { asyncResult: true },
  run: { asyncResult: true },
  jobStatus: { asyncResult: true },
  remove: { asyncResult: true },
});

type InternalState = {};

type CreateSearchResponse = {
  search: Search,
};
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

export type SearchJobActionsType = {
  create: (Search) => Promise<CreateSearchResponse>,
  run: (Search, SearchExecutionState) => Promise<SearchJobType>,
  jobStatus: (SearchJobId) => Promise<SearchJobType>,
};

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

  create(searchRequest: Search): Promise<CreateSearchResponse> {
    const promise = fetch('POST', createSearchUrl, JSON.stringify(searchRequest))
      .then((response) => {
        const search = Search.fromJSON(response);
        return { search: search };
      });
    SearchJobActions.create.promise(promise);
  },

  run(search: Search, executionState: SearchExecutionState): Promise<SearchJobType> {
    const promise = fetch('POST', executeQueryUrl(search.id), JSON.stringify(executionState));
    SearchJobActions.run.promise(promise);
  },

  jobStatus(jobId: SearchJobId): Promise<SearchJobType> {
    const promise = fetch('GET', jobStatusUrl(jobId));
    SearchJobActions.jobStatus.promise(promise);
  },

});
