import Reflux from 'reflux';
import { isEqual } from 'lodash';

import { singletonStore } from 'views/logic/singleton';

import { QueriesStore } from './QueriesStore';
import { ViewStore } from './ViewStore';

// eslint-disable-next-line import/prefer-default-export
export const CurrentQueryStore = singletonStore(
  'views.CurrentQuery',
  () => Reflux.createStore({
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
  }),
);
