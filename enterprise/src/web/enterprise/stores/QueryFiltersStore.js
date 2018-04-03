import Reflux from 'reflux';
import Immutable from 'immutable';

import QueryFiltersActions from '../actions/QueryFiltersActions';
import CurrentViewStore from './CurrentViewStore';

export default Reflux.createStore({
  listenables: [QueryFiltersActions],
  queryFilters: new Immutable.Map(),
  selectedView: undefined,

  init() {
    this.listenTo(CurrentViewStore, this.onCurrentViewStoreChange, this.onCurrentViewStoreChange);
  },

  getInitialState() {
    if (this.selectedView) {
      return this.queryFilters.get(this.selectedView, new Immutable.Map());
    }
    return new Immutable.Map();
  },

  onCurrentViewStoreChange(state) {
    if (this.selectedView !== state.selectedView) {
      this.selectedView = state.selectedView;
      this._trigger();
    }
  },

  streams(viewId, queryId, streams) {
    this.queryFilters = this.queryFilters.setIn([viewId, queryId, 'streams'], streams);
    this._trigger();
  },

  _trigger() {
    if (this.selectedView) {
      this.trigger(this.queryFilters.get(this.selectedView, new Immutable.Map()));
    } else {
      this.trigger(new Immutable.Map());
    }
  },
});
