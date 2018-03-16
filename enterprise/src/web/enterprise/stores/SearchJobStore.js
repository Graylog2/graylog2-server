import Reflux from 'reflux';
import _ from 'lodash';

import SearchJobActions from 'enterprise/actions/SearchJobActions';
import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';

import SearchRequest from 'enterprise/logic/SearchRequest';

const createSearchUrl = URLUtils.qualifyUrl('/plugins/org.graylog.plugins.enterprise/search');
const executeQueryUrl = id => URLUtils.qualifyUrl(`/plugins/org.graylog.plugins.enterprise/search/${id}/execute`);
const jobStatusUrl = jobId => URLUtils.qualifyUrl(`/plugins/org.graylog.plugins.enterprise/search/status/${jobId}`);

export default Reflux.createStore({
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
    const promise = fetch('POST', createSearchUrl, searchRequest.toRequest())
      .then((search) => {
        this.state.searches[search.id] = search;
        this._trigger();
        return { request: searchRequest, search: search };
      });
    SearchJobActions.create.promise(promise);
  },

  run(searchId, executionState) {
    const query = this.state.searches[searchId];
    if (!query) {
      SearchJobActions.run.promise(Promise.reject());
    } else {
      const promise = fetch('POST', executeQueryUrl(searchId), executionState || {})
        .then((job) => {
          this.state.jobs[job.id] = job;
          this._trigger();
          return job;
        });
      SearchJobActions.run.promise(promise);
    }
  },

  jobStatus(jobId) {
    const promise = fetch('GET', jobStatusUrl(jobId))
      .then((jobStatus) => {
        // transform the queryResult to something more usable and also check the structure here.
        const [id, searchId, done, failed] = _.at(jobStatus, ['id', 'search_id', 'execution.done', 'execution.complete_exceptionally']);
        this.state.jobs[id] = {
          id: id,
          searchId: searchId,
          done: done,
          failed: failed,
          results: jobStatus.results,
        };
        this._trigger();
        return jobStatus;
      });
    SearchJobActions.jobStatus.promise(promise);
  },

  remove(id) {
    delete this.state.searches[id];
    this._trigger();
  },

  _trigger() {
    this.trigger(this.state);
  },

});
