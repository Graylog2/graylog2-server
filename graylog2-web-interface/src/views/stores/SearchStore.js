/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
// @flow strict
import Reflux from 'reflux';
import Bluebird from 'bluebird';
import { debounce, get, isEqual } from 'lodash';

import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';
import { SearchMetadataActions } from 'views/stores/SearchMetadataStore';
import { SearchJobActions } from 'views/stores/SearchJobStore';
import { ViewStore, ViewActions } from 'views/stores/ViewStore';
import SearchResult from 'views/logic/SearchResult';
import SearchActions from 'views/actions/SearchActions';
import Search from 'views/logic/search/Search';
import type { CreateSearchResponse, SearchId, SearchExecutionResult } from 'views/actions/SearchActions';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import type { MessageListOptions } from 'views/logic/search/GlobalOverride';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import View from 'views/logic/views/View';
import Parameter from 'views/logic/parameters/Parameter';
import type { WidgetMapping } from 'views/logic/views/types';
import type { TimeRange } from 'views/logic/queries/Query';
import { singletonStore } from 'views/logic/singleton';

const createSearchUrl = qualifyUrl('/views/search');

const displayError = (error) => {
  // eslint-disable-next-line no-console
  console.error(error);
};

Bluebird.config({ cancellation: true });

const searchUrl = qualifyUrl('/views/search');

export { SearchActions };

export type SearchStoreState = {
  search: Search,
  result: SearchResult,
  widgetMapping: WidgetMapping,
};

export const SearchStore = singletonStore(
  'views.Search',
  () => Reflux.createStore({
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
          .then((jobStatus) => this.trackJobStatus(jobStatus, search)));
      });
    },

    trackJob(search: Search, executionState: SearchExecutionState): Promise<SearchResult> {
      return SearchJobActions.run(search, executionState).then((job) => this.trackJobStatus(job, search));
    },

    execute(executionState: SearchExecutionState): Promise<SearchExecutionResult> {
      const handleSearchResult = (searchResult: SearchResult) => searchResult;
      const startActionPromise = (executePromise) => SearchActions.execute.promise(executePromise);

      return this._executePromise(executionState, startActionPromise, handleSearchResult);
    },

    reexecuteSearchTypes(searchTypes: MessageListOptions, effectiveTimerange?: TimeRange): Promise<SearchExecutionResult> {
      const { parameterBindings, globalOverride } = this.executionState;
      const searchTypeIds = Object.keys(searchTypes);
      const globalQuery = globalOverride && globalOverride.query ? globalOverride.query : undefined;

      const newGlobalOverride: GlobalOverride = new GlobalOverride(
        effectiveTimerange,
        globalQuery,
        searchTypeIds,
        searchTypes,
      );

      const executionState = new SearchExecutionState(parameterBindings, newGlobalOverride);

      const handleSearchResult = (searchResult: SearchResult): SearchResult => {
        const updatedSearchTypes = searchResult.getSearchTypesFromResponse(searchTypeIds);
        const updatedResult = this.result.updateSearchTypes(updatedSearchTypes);

        return updatedResult;
      };

      const startActionPromise = (executePromise) => SearchActions.reexecuteSearchTypes.promise(executePromise);

      return this._executePromise(executionState, startActionPromise, handleSearchResult);
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

    _executePromise(executionState: SearchExecutionState, startActionPromise: (promise: Promise<SearchResult>) => void, handleSearchResult: (result: SearchResult) => SearchResult): Promise<SearchExecutionResult> {
      if (this.executePromise && this.executePromise.cancel) {
        this.executePromise.cancel();
      }

      if (this.search) {
        const { widgetMapping, search } = this.view;

        this.executePromise = this.trackJob(search, executionState)
          .then((result: SearchResult) => {
            this.result = handleSearchResult(result);
            this.widgetMapping = widgetMapping;
            this._trigger();
            this.executePromise = undefined;

            return { result, widgetMapping };
          }, displayError);

        startActionPromise(this.executePromise);

        return this.executePromise;
      }

      throw new Error('Unable to execute search when no search is loaded!');
    },

    _state(): SearchStoreState {
      return { search: this.search, result: this.result, widgetMapping: this.widgetMapping };
    },

    _trigger() {
      this.trigger(this._state());
    },
  }),
);
