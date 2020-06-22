import Reflux from 'reflux';
import { isEqual } from 'lodash';

import { singletonStore } from 'views/logic/singleton';

import { QueriesStore } from './QueriesStore';

// eslint-disable-next-line import/prefer-default-export
export const QueryIdsStore = singletonStore(
  'views.QueryIds',
  () => Reflux.createStore({
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
  }),
);
