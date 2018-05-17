import Reflux from 'reflux';

import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';
import Search from '../logic/search/Search';

const createSearchUrl = URLUtils.qualifyUrl('/plugins/org.graylog.plugins.enterprise/search');
const executeQueryUrl = id => URLUtils.qualifyUrl(`/plugins/org.graylog.plugins.enterprise/search/${id}/execute`);
const jobStatusUrl = jobId => URLUtils.qualifyUrl(`/plugins/org.graylog.plugins.enterprise/search/status/${jobId}`);


export const SearchJobActions = Reflux.createActions({
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

  getInitialState() {
    return {
      searches: this.state.searches,
      jobs: this.state.jobs,
    };
  },

  create(searchRequest) {
    const promise = fetch('POST', createSearchUrl, JSON.stringify(searchRequest))
      .then((response) => {
        const search = Search.fromJSON(response);
        return { search: search };
      });
    SearchJobActions.create.promise(promise);
  },

  run(search, executionState) {
    const executionStateValue = executionState ? executionState.toJS() : {};
    const promise = fetch('POST', executeQueryUrl(search.id), executionStateValue);
    SearchJobActions.run.promise(promise);
  },

  jobStatus(jobId) {
    const promise = fetch('GET', jobStatusUrl(jobId));
    SearchJobActions.jobStatus.promise(promise);
  },

});
