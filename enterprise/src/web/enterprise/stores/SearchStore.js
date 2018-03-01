import Reflux from 'reflux';
import Immutable from 'immutable';

import SearchJobActions from 'enterprise/actions/SearchJobActions';
import SearchJobStore from 'enterprise/stores/SearchJobStore';
import SearchActions from 'enterprise/actions/SearchActions';
import SearchRequest from 'enterprise/logic/SearchRequest';
import SearchResult from 'enterprise/logic/SearchResult';
import WidgetStore from './WidgetStore';
import QueriesStore from './QueriesStore';

const displayError = (error) => {
  console.log(error);
};


export default Reflux.createStore({
  listenables: [SearchActions],
  state: new Immutable.Map(),

  init() {
    this.listenTo(WidgetStore, this.onWidgetStoreUpdate, this.onWidgetStoreUpdate);
    this.listenTo(QueriesStore, this.onQueriesStoreUpdate, this.onQueriesStoreUpdate);
  },

  onWidgetStoreUpdate(widgets) {
    this.widgets = widgets;
  },
  onQueriesStoreUpdate(queries) {
    this.queries = queries;
  },

  trackJobStatus(job, searchRequest, search) {
    return new Promise((resolve, reject) => {
      if (job && job.execution.done) {
        resolve(new SearchResult(searchRequest, job));
      } else {
        if (this.trackTimeout) {
          clearTimeout(this.trackTimeout);
        }
        this.trackTimeout = setTimeout(() => SearchJobActions.jobStatus(job.id)
          .then(jobStatus => resolve(this.trackJobStatus(jobStatus, searchRequest, search)), error => reject(error)), 250);
      }
    });
  },

  trackJob(search, searchRequest) {
    return SearchJobActions.run(search.id).then((job) => {
      return this.trackJobStatus(job, searchRequest, search);
    });
  },

  execute() {
    const searchRequest = new SearchRequest(this.queries, this.widgets);
    const promise = SearchJobActions.create(searchRequest)
      .then((search) => {
        return this.trackJob(search, searchRequest);
      }, displayError)
      .then((result) => {
        this.state = this.state.set('result', result);
        this._trigger();
      }, displayError);

    SearchActions.execute.promise(promise);
  },

  _trigger() {
    this.trigger(this.state);
  }
});