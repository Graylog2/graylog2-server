import Reflux from 'reflux';
import Immutable from 'immutable';
import { isEqual } from 'lodash';
import moment from 'moment';

import { ViewActions, ViewStore } from './ViewStore';
import Search from '../logic/search/Search';
import { QueriesActions } from '../actions/QueriesActions';

export { QueriesActions } from '../actions/QueriesActions';
export const QueriesStore = Reflux.createStore({
  listenables: [QueriesActions],
  queries: new Immutable.Map(),
  search: Search.create(),

  init() {
    this.listenTo(ViewStore, this.onViewStoreChange, this.onViewStoreChange);
  },

  getInitialState() {
    return this._state();
  },

  onViewStoreChange(state) {
    const { view } = state;
    if (!view || !view.search) {
      return;
    }
    const { search } = view;
    this.search = search;

    const { queries } = search;
    const newQueries = Immutable.Map(queries.map(q => [q.id, q]));

    if (!isEqual(newQueries, this.queries)) {
      this.queries = newQueries;
      this._trigger();
    }
  },

  remove(queryId) {
    const newQueries = this.queries.remove(queryId);
    const promise = this._propagateQueryChange(newQueries);
    QueriesActions.remove.promise(promise);
    return promise;
  },
  update(queryId, query) {
    const newQueries = this.queries.set(queryId, query);
    const promise = this._propagateQueryChange(newQueries);
    QueriesActions.update.promise(promise);
    return promise;
  },

  query(queryId, query) {
    const activeQuery = this.queries.get(queryId);
    const newQuery = activeQuery.toBuilder().query(Object.assign({}, activeQuery.query, { query_string: query })).build();
    const newQueries = this.queries.set(queryId, newQuery);
    const promise = this._propagateQueryChange(newQueries);
    QueriesActions.query.promise(promise);
    return promise;
  },
  timerange(queryId, timerange) {
    const newQueries = this.queries.update(queryId, query => query.toBuilder().timerange(timerange).build());
    const promise = this._propagateQueryChange(newQueries);
    QueriesActions.timerange.promise(promise);
    return promise;
  },
  rangeParams(queryId, key, value) {
    const newQueries = this.queries.update(queryId, query => query.toBuilder().timerange(Object.assign({}, query.timerange, { [key]: value })).build());
    const promise = this._propagateQueryChange(newQueries);
    QueriesActions.rangeParams.promise(promise);
    return promise;
  },
  rangeType(queryId, type) {
    const oldQuery = this.queries.get(queryId);
    const oldTimerange = oldQuery.timerange;
    const oldType = oldTimerange.type;

    if (type === oldType) {
      return Promise.resolve();
    }

    const newTimerange = { type };

    // eslint-disable-next-line default-case
    switch (type) {
      case 'absolute':
        newTimerange.from = moment().subtract(oldTimerange.range, 'seconds').toISOString();
        newTimerange.to = moment().toISOString();
        break;
      case 'relative':
        newTimerange.range = 300;
        break;
      case 'keyword':
        newTimerange.keyword = 'Last five Minutes';
        break;
    }
    const newQueries = this.queries.update(queryId, query => query.toBuilder().timerange(newTimerange).build());
    const promise = this._propagateQueryChange(newQueries);
    QueriesActions.rangeType.promise(promise);
    return promise;
  },

  _propagateQueryChange(newQueries) {
    const newSearch = this.search.toBuilder()
      .queries(newQueries.valueSeq().toList())
      .build();
    return ViewActions.search(newSearch);
  },

  _state() {
    return this.queries;
  },

  _trigger() {
    this.trigger(this._state());
  },
});
