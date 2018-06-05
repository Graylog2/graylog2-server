import Reflux from 'reflux';
import Immutable from 'immutable';
import { isEqual } from 'lodash';

import { QueriesActions, QueriesStore } from './QueriesStore';

const _streamFilters = (selectedStreams) => {
  return selectedStreams.map(stream => ({ type: 'stream', id: stream }));
};

const _filtersForQuery = (streams) => {
  const streamFilters = _streamFilters(streams);
  if (streamFilters.length === 0) {
    return null;
  }

  return {
    type: 'or',
    filters: streamFilters,
  };
};

export const QueryFiltersActions = Reflux.createActions([
  'streams',
]);

export const QueryFiltersStore = Reflux.createStore({
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
    const streamFilter = _filtersForQuery(streams);
    const newQuery = this.queries.get(queryId).toBuilder().filter(streamFilter).build();
    QueriesActions.update(queryId, newQuery);
  },

  _state() {
    return this.queries.map(q => q.filter).filter(f => f !== undefined);
  },
  _trigger() {
    this.trigger(this._state());
  },
});
