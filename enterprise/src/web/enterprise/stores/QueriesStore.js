import Reflux from 'reflux';
import Immutable from 'immutable';
import { isEqual } from 'lodash';

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
    this._propagateQueryChange(newQueries);
  },
  update(queryId, query) {
    const newQueries = this.queries.set(queryId, query);
    this._propagateQueryChange(newQueries);
  },

  query(queryId, query) {
    const activeQuery = this.queries.get(queryId);
    const newQuery = activeQuery.toBuilder().query(Object.assign({}, activeQuery.query, { query_string: query })).build();
    const newQueries = this.queries.set(queryId, newQuery);
    this._propagateQueryChange(newQueries);
  },
  rangeParams(queryId, key, value) {
    const newQueries = this.queries.update(queryId, query => query.toBuilder().timerange(Object.assign({}, query.timerange, { [key]: value })).build());
    this._propagateQueryChange(newQueries);
  },
  rangeType(queryId, type) {
    const newQueries = this.queries.update(queryId, query => query.toBuilder().timerange(Object.assign({}, query.timerange, { type })).build());
    this._propagateQueryChange(newQueries);
  },

  _propagateQueryChange(newQueries) {
    const newSearch = this.search.toBuilder().queries(newQueries.valueSeq().toList()).build();
    return ViewActions.search(newSearch);
  },

  _state() {
    return this.queries;
  },

  _trigger() {
    this.trigger(this._state());
  },
});
