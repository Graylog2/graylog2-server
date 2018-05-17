import Reflux from 'reflux';
import Immutable from 'immutable';

import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';
import { QueryFiltersStore } from './QueryFiltersStore';

const fieldTypesUrl = URLUtils.qualifyUrl('/plugins/org.graylog.plugins.enterprise/fields');

export const FieldTypesActions = Reflux.createActions({
  all: { asyncResult: true },
});

export const FieldTypesStore = Reflux.createStore({
  listenables: [FieldTypesActions],

  init() {
    this.all();
    this.listenTo(QueryFiltersStore, this.onQueryFiltersUpdate, this.onQueryFiltersUpdate);
  },

  getInitialState() {
    return this._state();
  },

  onQueryFiltersUpdate(newFilters) {
    const promises = newFilters
      .filter(filter => filter !== undefined && filter !== null)
      .map(filter => filter.get('filters', Immutable.List()).filter(f => f.get('type') === 'stream').map(f => f.get('id')))
      .filter(streamFilters => streamFilters.size > 0)
      .map((filters, queryId) => this.forStreams(filters.toArray()).then(response => ({
        queryId,
        response,
      })))
      .valueSeq()
      .toJS();

    Promise.all(promises).then((results) => {
      const combinedResult = {};
      results.forEach(({ queryId, response }) => {
        combinedResult[queryId] = response;
      });
      this.queryFields = Immutable.fromJS(combinedResult);
      this._trigger();
    });
  },

  all() {
    const promise = fetch('GET', fieldTypesUrl).then((response) => {
      this.all = Immutable.fromJS(response);
      this._trigger();
    });

    FieldTypesActions.all.promise(promise);

    return promise;
  },

  forStreams(streams) {
    return fetch('POST', fieldTypesUrl, { streams: streams });
  },

  _state() {
    return {
      all: this.all,
      queryFields: this.queryFields,
    };
  },
  _trigger() {
    this.trigger(this._state());
  },
});
