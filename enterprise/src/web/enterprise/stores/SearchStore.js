import Reflux from 'reflux';
import Bluebird from 'bluebird';
import _ from 'lodash';

import SearchMetadataActions from 'enterprise/actions/SearchMetadataActions';
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

Bluebird.config({ cancellation: true });

export default Reflux.createStore({
  listenables: [SearchActions],
  executePromise: null,

  init() {
    this.listenTo(WidgetStore, this.onWidgetStoreUpdate, this.onWidgetStoreUpdate);
    this.listenTo(QueriesStore, this.onQueriesStoreUpdate, this.onQueriesStoreUpdate);
  },

  _debouncedParse: _.debounce((queries, widgets) => {
    const search = new SearchRequest(queries, widgets);
    console.log('Parsing search', search);
    SearchMetadataActions.parseSearch(search)
      .then(metadata => console.log(metadata));
  }, 500),

  onWidgetStoreUpdate(widgets) {
    this.widgets = widgets;
    this.onUpdate();
  },
  onQueriesStoreUpdate(queries) {
    this.queries = queries;
    this.onUpdate();
  },

  onUpdate() {
    if (this.queries && this.queries.size > 0) {
      this._debouncedParse(this.queries, this.widgets);
    }
  },

  trackJobStatus(job, searchRequest, search) {
    return new Bluebird((resolve) => {
      if (job && job.execution.done) {
        return resolve(new SearchResult(searchRequest, job));
      }
      return resolve(Bluebird.delay(250)
        .then(() => SearchJobActions.jobStatus(job.id))
        .then(jobStatus => this.trackJobStatus(jobStatus, searchRequest, search)));
    });
  },

  trackJob(search, searchRequest) {
    return SearchJobActions.run(search.id).then(job => this.trackJobStatus(job, searchRequest, search));
  },

  execute() {
    if (this.executePromise) {
      this.executePromise.cancel();
    }
    const searchRequest = new SearchRequest(this.queries, this.widgets);
    this.executePromise = SearchJobActions.create(searchRequest)
      .then(({ request, search }) => this.trackJob(search, request), displayError)
      .then((result) => {
        this.trigger({ result });
        this.executePromise = undefined;
        return result;
      }, displayError);

    SearchActions.execute.promise(this.executePromise);
  },
});