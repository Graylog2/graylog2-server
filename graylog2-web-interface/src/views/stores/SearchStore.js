// @flow strict
import Reflux from 'reflux';
import Bluebird from 'bluebird';
import { debounce, get, isEqual } from 'lodash';

// $FlowFixMe: imports from core need to be fixed in flow
import URLUtils from 'util/URLUtils';
// $FlowFixMe: imports from core need to be fixed in flow
import fetch from 'logic/rest/FetchProvider';

import { SearchExecutionStateStore } from 'enterprise/stores/SearchExecutionStateStore';
import { SearchMetadataActions } from 'enterprise/stores/SearchMetadataStore';
import { SearchJobActions } from 'enterprise/stores/SearchJobStore';
import { ViewStore, ViewActions } from 'enterprise/stores/ViewStore';
import SearchResult from 'enterprise/logic/SearchResult';
import SearchActions from 'enterprise/actions/SearchActions';
import Search from 'enterprise/logic/search/Search';
import type { CreateSearchResponse, SearchId, SearchExecutionResult } from 'enterprise/actions/SearchActions';
import SearchExecutionState from 'enterprise/logic/search/SearchExecutionState';
import View from 'enterprise/logic/views/View';
import Parameter from 'enterprise/logic/parameters/Parameter';
import type { WidgetMapping } from 'enterprise/logic/views/View';

const createSearchUrl = URLUtils.qualifyUrl('/views/search');

const displayError = (error) => {
  // eslint-disable-next-line no-console
  console.error(error);
};

Bluebird.config({ cancellation: true });

const searchUrl = URLUtils.qualifyUrl('/views/search');

export { SearchActions };

type InternalState = {
  search: Search,
  result: SearchResult,
  widgetMapping: WidgetMapping,
};

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

  onViewStoreUpdate({ view }: { view: View }) {
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

  create(searchRequest: Search): Promise<CreateSearchResponse> {
    const newSearch = searchRequest.toBuilder().newId().build();
    const promise = fetch('POST', createSearchUrl, JSON.stringify(newSearch))
      .then((response) => {
        const search = Search.fromJSON(response);
        return { search: search };
      });
    SearchActions.create.promise(promise);
    return promise;
  },

  get(searchId: SearchId): Promise<Search> {
    const promise = fetch('GET', `${searchUrl}/${searchId}`);
    SearchActions.get.promise(promise);
    return promise;
  },

  trackJobStatus(job, search: Search) {
    return new Bluebird((resolve) => {
      if (job && job.execution.done) {
        return resolve(new SearchResult(job));
      }
      return resolve(Bluebird.delay(250)
        .then(() => SearchJobActions.jobStatus(job.id))
        .then(jobStatus => this.trackJobStatus(jobStatus, search)));
    });
  },

  trackJob(search: Search, executionState: SearchExecutionState): Promise<SearchResult> {
    return SearchJobActions.run(search, executionState).then(job => this.trackJobStatus(job, search));
  },

  execute(executionState: SearchExecutionState): Promise<SearchExecutionResult> {
    if (this.executePromise) {
      this.executePromise.cancel();
    }
    if (this.search) {
      const { widgetMapping, search } = this.view;
      this.executePromise = this.trackJob(search, executionState)
        .then((result) => {
          this.result = result;
          this.widgetMapping = widgetMapping;
          this._trigger();
          this.executePromise = undefined;
          return { result, widgetMapping };
        }, displayError);
      SearchActions.execute.promise(this.executePromise);
      return this.executePromise;
    }
    throw new Error('Unable to execute search when no search is loaded!');
  },

  executeWithCurrentState(): Promise<SearchExecutionResult> {
    const promise = SearchActions.execute(this.executionState);
    SearchActions.executeWithCurrentState.promise(promise);
    return promise;
  },

  parameters(newParameters: Array<Parameter>): Promise<View> {
    const newSearch = this.search.toBuilder().parameters(newParameters).build();
    const promise = ViewActions.search(newSearch);
    SearchActions.parameters.promise(promise);
    return promise;
  },
  _state(): InternalState {
    return { search: this.search, result: this.result, widgetMapping: this.widgetMapping };
  },
  _trigger() {
    this.trigger(this._state());
  },
});
