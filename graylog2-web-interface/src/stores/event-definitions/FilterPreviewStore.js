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
import Reflux from 'reflux';
import URI from 'urijs';
import lodash from 'lodash';
import Bluebird from 'bluebird';

import * as URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import CombinedProvider from 'injection/CombinedProvider';
import Search from 'views/logic/search/Search';
import SearchResult from 'views/logic/SearchResult';

const { FilterPreviewActions } = CombinedProvider.get('FilterPreview');

const FilterPreviewStore = Reflux.createStore({
  listenables: [FilterPreviewActions],
  sourceUrl: '/views/search',
  searchJob: undefined,
  result: undefined,

  getInitialState() {
    return this.getState();
  },

  propagateChanges() {
    this.trigger(this.getState());
  },

  getState() {
    return {
      searchJob: this.searchJob,
      result: this.result,
    };
  },

  resourceUrl({ segments = [], query = {} }) {
    const uri = new URI(this.sourceUrl);
    const nextSegments = lodash.concat(uri.segment(), segments);

    uri.segmentCoded(nextSegments);
    uri.query(query);

    return URLUtils.qualifyUrl(uri.resource());
  },

  /**
   * Method that creates a search query in the backend. This method does not execute the search, please call
   * `execute()` once the response of `create()` is resolved to execute the search.
   */
  create(searchRequest) {
    const newSearch = searchRequest.toBuilder()
      .newId()
      .build();
    const promise = fetch('POST', this.resourceUrl({}), JSON.stringify(newSearch));

    promise.then((response) => {
      this.searchJob = Search.fromJSON(response);
      this.result = undefined;
      this.propagateChanges();

      return response;
    });

    FilterPreviewActions.create.promise(promise);
  },

  trackJobStatus(job, search) {
    return new Bluebird((resolve) => {
      if (job && job.execution.done) {
        return resolve(new SearchResult(job));
      }

      return resolve(Bluebird.delay(250)
        .then(() => this.jobStatus(job.id))
        .then((jobStatus) => this.trackJobStatus(jobStatus, search)));
    });
  },

  run(search, executionState) {
    return fetch('POST', this.resourceUrl({ segments: [search.id, 'execute'] }), JSON.stringify(executionState));
  },

  jobStatus(jobId) {
    return fetch('GET', this.resourceUrl({ segments: ['status', jobId] }));
  },

  trackJob(search, executionState) {
    return this.run(search, executionState).then((job) => this.trackJobStatus(job, search));
  },

  /**
   * Method that executes a search in the backend and wait until its results are ready.
   * Take into account that you need to create the search before you execute it.
   */
  execute(executionState) {
    if (this.executePromise) {
      this.executePromise.cancel();
    }

    if (this.searchJob) {
      this.executePromise = this.trackJob(this.searchJob, executionState)
        .then(
          (result) => {
            this.result = result;
            this.executePromise = undefined;
            this.propagateChanges();

            return result;
          },
          () => UserNotification.error('Could not execute search, wat'),
        );

      FilterPreviewActions.execute.promise(this.executePromise);

      return this.executePromise;
    }

    throw new Error('Unable to execute search if no search was created before!');
  },

  search(searchRequest, executionState) {
    FilterPreviewActions.create(searchRequest)
      .then(() => FilterPreviewActions.execute(executionState));
  },
});

export default FilterPreviewStore;
