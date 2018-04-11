import Reflux from 'reflux';
import Bluebird from 'bluebird';
import _ from 'lodash';

import SearchMetadataActions from 'enterprise/actions/SearchMetadataActions';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import SearchJobActions from 'enterprise/actions/SearchJobActions';
import SearchJobStore from 'enterprise/stores/SearchJobStore';
import SearchParameterStore from 'enterprise/stores/SearchParameterStore';
import SearchActions from 'enterprise/actions/SearchActions';
import SearchRequest from 'enterprise/logic/SearchRequest';
import SearchResult from 'enterprise/logic/SearchResult';
import WidgetStore from './WidgetStore';
import QueriesStore from './QueriesStore';
import QueryFiltersStore from './QueryFiltersStore';

const displayError = (error) => {
  console.log(error);
};

Bluebird.config({ cancellation: true });

const searchUrl = URLUtils.qualifyUrl('/plugins/org.graylog.plugins.enterprise/search');

export default Reflux.createStore({
  listenables: [SearchActions],
  executePromise: null,

  init() {
    this.listenTo(WidgetStore, this.onWidgetStoreUpdate, this.onWidgetStoreUpdate);
    this.listenTo(QueriesStore, this.onQueriesStoreUpdate, this.onQueriesStoreUpdate);
    this.listenTo(QueryFiltersStore, this.onQueryFiltersStoreUpdate, this.onQueryFiltersStoreUpdate);
    this.listenTo(SearchParameterStore, this.onSearchParameterUpdate, this.onSearchParameterUpdate);
  },

  _debouncedParse: _.debounce((queries, parameters, widgets, filters) => {
    const search = new SearchRequest(queries, parameters, widgets, filters);
    SearchMetadataActions.parseSearch(search);
  }, 500),

  onWidgetStoreUpdate(widgets) {
    this.widgets = widgets;
    this.onUpdate();
  },
  onQueriesStoreUpdate(queries) {
    this.queries = queries;
    this.onUpdate();
  },
  onQueryFiltersStoreUpdate(filters) {
    this.filters = filters;
    this.onUpdate();
  },
  onSearchParameterUpdate(parameters) {
    this.parameters = parameters;
    this.onUpdate();
  },

  onUpdate() {
    if (this.queries && this.queries.size > 0) {
      this._debouncedParse(this.queries, this.parameters, this.widgets, this.filters);
    }
  },

  get(searchId) {
    const promise = fetch('GET', `${searchUrl}/${searchId}`);
    SearchActions.get.promise(promise);
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

  trackJob(search, searchRequest, executionState) {
    return SearchJobActions.run(search.id, executionState).then(job => this.trackJobStatus(job, searchRequest, search));
  },

  execute(executionState) {
    if (this.executePromise) {
      this.executePromise.cancel();
    }
    const searchRequest = new SearchRequest(this.queries, this.parameters, this.widgets, this.filters);
    this.executePromise = SearchJobActions.create(searchRequest)
      .then(({ request, search }) => this.trackJob(search, request, executionState), displayError)
      .then((result) => {
        this.trigger({ result });
        this.executePromise = undefined;
        return result;
      }, displayError);

    SearchActions.execute.promise(this.executePromise);
  },
});