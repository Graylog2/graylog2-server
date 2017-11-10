import Reflux from 'reflux';

import QueriesActions from 'enterprise/actions/QueriesActions';
import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';
import ObjectUtils from 'util/ObjectUtils';

const createSearchUrl = URLUtils.qualifyUrl('/plugins/org.graylog.plugins.enterprise/search');
const executeQueryUrl = id => URLUtils.qualifyUrl(`/plugins/org.graylog.plugins.enterprise/search/${id}/execute`);
const jobStatusUrl = jobId => URLUtils.qualifyUrl(`/plugins/org.graylog.plugins.enterprise/search/status/${jobId}`);

export default Reflux.createStore({
  listenables: [QueriesActions],

  state: {
    queries: {},
    jobs: {},
  },

  getInitialState() {
    return { queries: this.state.queries, jobs: this.state.jobs };
  },

  create(query) {
    const promise = fetch('POST', createSearchUrl, query)
      .then((savedQuery) => {
        this.state.queries[savedQuery.id] = savedQuery;
        return fetch('POST', executeQueryUrl(savedQuery.id));
      });
    QueriesActions.create.promise(promise);
  },

  run(queryId, executionState = {}) {
    const query = this.state.queries[queryId];
    if (!query) {
      QueriesActions.run.promise(Promise.reject());
    } else {
      const promise = fetch('POST', executeQueryUrl(queryId), executionState)
        .then((job) => {
          // dummy object until we polled the result for the first time
          this.state.jobs[job.id] = { job: { done: false } };
          this._trigger();
        });
      QueriesActions.run.promise(promise);
    }
  },

  jobStatus(jobId) {
    const promise = fetch('GET', jobStatusUrl(jobId))
      .then((queryResult) => {
        this.state.jobs[queryResult.job.id] = queryResult;
        this._trigger();
      });
    QueriesActions.jobStatus.promise(promise);
  },

  /*

      .then(executionResponse => fetch('GET', jobStatusUrl(executionResponse.job_id)))
      .then((result) => {
        const searchResult = ObjectUtils.clone(result);
        searchResult.results.messages.fields = ['source', 'message'];
        searchResult.results.messages.used_indices = [];
        searchResult.results.messages.built_query = '"*"';
        this.queries[searchResult.query.id] = searchResult;
        this._trigger();
      });
   */

  remove(id) {
    delete this.state.queries[id];
    this._trigger();
  },

  _trigger() {
    this.trigger(this.state);
  },

});
