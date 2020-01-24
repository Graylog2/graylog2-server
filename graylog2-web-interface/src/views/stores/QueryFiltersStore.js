import Reflux from 'reflux';
import Immutable from 'immutable';
import { isEqual } from 'lodash';

import { singletonActions, singletonStore } from 'views/logic/singleton';
import { filtersForQuery } from 'views/logic/queries/Query';
import { QueriesActions, QueriesStore } from './QueriesStore';


export const QueryFiltersActions = singletonActions(
  'views.QueryFilters',
  () => Reflux.createActions({
    streams: { asyncResult: true },
  }),
);

export const QueryFiltersStore = singletonStore(
  'views.QueryFilters',
  () => Reflux.createStore({
    listenables: [QueryFiltersActions],
    queries: Immutable.Map(),

    init() {
      this.listenTo(QueriesStore, this.onQueriesStoreChange, this.onQueriesStoreChange);
    },

    getInitialState() {
      return this._state();
    },
    onQueriesStoreChange(newQueries) {
      const newFilters = newQueries.map(q => q.filter);
      const oldFilters = this.queries.map(q => q.filter);
      this.queries = newQueries;
      if (!isEqual(newFilters, oldFilters)) {
        this._trigger();
      }
    },

    streams(queryId, streams) {
      const streamFilter = filtersForQuery(streams);
      const newQuery = this.queries.get(queryId).toBuilder().filter(streamFilter).build();
      const promise = QueriesActions.update(queryId, newQuery);
      QueryFiltersActions.streams.promise(promise);
      return promise;
    },

    _state() {
      return this.queries.map(q => q.filter).filter(f => f !== undefined);
    },
    _trigger() {
      this.trigger(this._state());
    },
  }),
);
