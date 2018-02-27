import Reflux from 'reflux';
import Immutable from 'immutable';

import QueriesActions from 'enterprise/actions/QueriesActions';
import CurrentViewStore from './CurrentViewStore';

export default Reflux.createStore({
  listenables: [QueriesActions],
  queries: new Immutable.Map(),
  selectedView: undefined,

  init() {
    this.listenTo(CurrentViewStore, this.onCurrentViewStoreChange, this.onCurrentViewStoreChange);
  },

  getInitialState() {
    if (this.selectedView) {
      return this.queries.get(this.selectedView);
    }
    return new Immutable.Map();
  },

  onCurrentViewStoreChange(state) {
    if (this.selectedView !== state.selectedView) {
      this.selectedView = state.selectedView;
      this._trigger();
    }
  },

  create(viewId, query) {
    if (query.id === undefined) {
      throw new Error('Unable to add query without id to view.');
    }
    this.queries = this.queries.updateIn([viewId, query.id], (value) => {
      if (value !== undefined) {
        throw new Error(`Unable to add query with id <${query.id}>, it is already present in view with id <${viewId}>.`);
      }
      return new Immutable.Map(query);
    });
    this._trigger();
  },
  remove(viewId, queryId) {
    this.queries = this.queries.removeIn([viewId, queryId]);
    this._trigger();
  },
  update(viewId, queryId, query) {
    this.queries = this.queries.setIn([viewId, queryId], new Immutable.Map(query));
    this._trigger();
  },

  query(viewId, queryId, query) {
    this.queries = this.queries.setIn([viewId, queryId, 'query'], query);
    this._trigger();
  },
  rangeParams(viewId, queryId, key, value) {
    this.queries = this.queries.setIn([viewId, queryId, 'rangeParams', key], value);
    this._trigger();
  },
  rangeType(viewId, queryId, type) {
    this.queries = this.queries.setIn([viewId, queryId, 'rangeType'], type);
    this._trigger();
  },

  toggleField(viewId, queryId, field) {
    if (this.queries.getIn([viewId, queryId, 'fields']).contains(field)) {
      this.removeField(viewId, queryId, field);
    } else {
      this.addField(viewId, queryId, field);
    }
  },

  addField(viewId, queryId, field) {
    this.queries = this.queries.updateIn([viewId, queryId, 'fields'], fields => fields.add(field));
    this._trigger();
  },

  removeField(viewId, queryId, field) {
    this.queries = this.queries.updateIn([viewId, queryId, 'fields'], fields => fields.delete(field));
    this._trigger();
  },

  _trigger() {
    if (this.selectedView) {
      this.trigger(this.queries.get(this.selectedView, new Immutable.Map()));
    } else {
      this.trigger(new Immutable.Map());
    }
  },
});