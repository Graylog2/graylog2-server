import Reflux from 'reflux';
import Bluebird from 'bluebird';
import { debounce, get, isEqual } from 'lodash';

import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

import { SearchMetadataActions } from 'enterprise/stores/SearchMetadataStore';
import { SearchJobActions } from 'enterprise/stores/SearchJobStore';
import { ViewStore, ViewActions } from 'enterprise/stores/ViewStore';
import SearchResult from 'enterprise/logic/SearchResult';
import { SearchExecutionStateStore } from './SearchExecutionStateStore';
import SearchActions from '../actions/SearchActions';

const displayError = (error) => {
  // eslint-disable-next-line no-console
  console.error(error);
};

Bluebird.config({ cancellation: true });

const searchUrl = URLUtils.qualifyUrl('/plugins/org.graylog.plugins.enterprise/search');

export { SearchActions };

export const SearchStore = Reflux.createStore({
  listenables: [SearchActions],
  executePromise: null,

  init() {
    this.listenTo(ViewStore, this.onViewStoreUpdate, this.onViewStoreUpdate);
    this.listenTo(SearchExecutionStateStore, this.onSearchExecutionStateUpdate, this.onSearchExecutionStateUpdate);
  },

  getInitialState() {
    return this._state();
  },

  _debouncedParse: debounce((search) => {
    SearchMetadataActions.parseSearch(search);
  }, 500),

  onViewStoreUpdate({ view }) {
    this.view = view;
    const search = get(view, 'search');
    if (!isEqual(this.search, search)) {
      this.search = search;
      this.onUpdate(search);
      this._trigger();
    }
  },
  onSearchExecutionStateUpdate(executionState) {
    this.executionState = executionState;
  },

  onUpdate(search) {
    const { queries } = search;
    if (queries && queries.size > 0) {
      this._debouncedParse(this.search);
    }
  },

  get(searchId) {
    const promise = fetch('GET', `${searchUrl}/${searchId}`);
    SearchActions.get.promise(promise);
  },

  trackJobStatus(job, search) {
    return new Bluebird((resolve) => {
      if (job && job.execution.done) {
        return resolve(new SearchResult(job));
      }
      return resolve(Bluebird.delay(250)
        .then(() => SearchJobActions.jobStatus(job.id))
        .then(jobStatus => this.trackJobStatus(jobStatus, search)));
    });
  },

  trackJob(search, executionState) {
    return SearchJobActions.run(search, executionState).then(job => this.trackJobStatus(job, search));
  },

  execute(executionState) {
    if (this.executePromise) {
      this.executePromise.cancel();
    }
    if (this.search) {
      const { widgetMapping } = this.view;
      this.executePromise = SearchJobActions.create(this.search)
        .then(({ search }) => this.trackJob(search, executionState), displayError)
        .then((result) => {
          this.result = result;
          this.widgetMapping = widgetMapping;
          this._trigger();
          this.executePromise = undefined;
          return { result, widgetMapping };
        }, displayError);
      SearchActions.execute.promise(this.executePromise);
    }
  },

  executeWithCurrentState() {
    const promise = SearchActions.execute(this.executionState);
    SearchActions.executeWithCurrentState.promise(promise);
    return promise;
  },

  parameters(newParameters) {
    const newSearch = this.search.toBuilder().parameters(newParameters).build();
    ViewActions.search(newSearch);
  },
  _state() {
    return { search: this.search, result: this.result, widgetMapping: this.widgetMapping };
  },
  _trigger() {
    this.trigger(this._state());
  },
});
