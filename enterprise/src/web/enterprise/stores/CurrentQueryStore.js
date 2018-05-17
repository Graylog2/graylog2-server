import Reflux from 'reflux';
import { isEqual } from 'lodash';

import { QueriesStore } from './QueriesStore';
import { ViewStore } from './ViewStore';

export const CurrentQueryStore = Reflux.createStore({
  init() {
    this.listenTo(ViewStore, this.onViewStoreUpdate, this.onViewStoreUpdate);
    this.listenTo(QueriesStore, this.onQueriesStoreUpdate, this.onQueriesStoreUpdate);
  },
  getInitialState() {
    return this._state();
  },
  onQueriesStoreUpdate(queries) {
    this.queries = queries;
    if (this.activeQuery) {
      const newQuery = queries.get(this.activeQuery);
      if (!isEqual(newQuery, this.query)) {
        this.query = newQuery;
        this._trigger();
      }
    }
  },
  onViewStoreUpdate({ activeQuery }) {
    if (!isEqual(activeQuery, this.activeQuery)) {
      this.activeQuery = activeQuery;
      this.query = this.queries.get(activeQuery);
      this._trigger();
    }
  },
  _state() {
    return this.query;
  },
  _trigger() {
    this.trigger(this._state());
  },
});
