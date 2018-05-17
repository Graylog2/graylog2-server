import Reflux from 'reflux';
import { isEqual } from 'lodash';

import { QueriesStore } from './QueriesStore';

export const QueryIdsStore = Reflux.createStore({
  state: {},
  init() {
    this.listenTo(QueriesStore, this.onQueriesStoreUpdate, this.onQueriesStoreUpdate);
  },
  getInitialState() {
    return this._state();
  },
  onQueriesStoreUpdate(queries) {
    const newState = queries.keySeq().toList();
    if (!isEqual(this.state, newState)) {
      this.state = newState;
      this._trigger();
    }
  },
  _state() {
    return this.state;
  },
  _trigger() {
    this.trigger(this._state());
  },
});
