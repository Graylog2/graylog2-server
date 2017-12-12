import Reflux from 'reflux';
import _ from 'lodash';
import uuidv4 from 'uuid/v4';

import SearchJobActions from 'enterprise/actions/SearchJobActions';
import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';

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

  create(queries) {
    // queries is the state of the view. We need to transform it to the correct format for the server
    const searchRequest = {
      queries: queries.map((query) => {
        const transformed = {
          id: query.id,
          // TODO create conversion objects for query objects
          query: {
            type: 'elasticsearch',
            query_string: query.query,
          },
          // TODO create conversion objects for timerange objects
          timerange: {
            type: query.rangeType,
            range: query.rangeParams.get('range'),
          },
          // TODO the view state should reflect what search types we will be requesting for each query
          search_types: [
            // for now always include messages as search type
            {
              type: 'messages',
              id: uuidv4(),
              limit: 150,
              offset: 0,
              sort: [{ field: 'timestamp', order: 'DESC' }],
            },
          ],
        };
        // console.log(query, transformed);
        return transformed;
      }),
    };
    const promise = fetch('POST', createSearchUrl, searchRequest)
      .then((search) => {
        this.state.searches[search.id] = search;
        this._trigger();
        return search;
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
          // dummy object until we polled the result for the first time
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
