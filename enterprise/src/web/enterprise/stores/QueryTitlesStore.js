import Reflux from 'reflux';
import Immutable from 'immutable';
import { get, isEqual } from 'lodash';

import { ViewStore } from './ViewStore';

export const QueryTitlesStore = Reflux.createStore({
  init() {
    this.listenTo(ViewStore, this.onViewStoreUpdate, this.onViewStoreUpdate);
  },
  getInitialState() {
    return this._state();
  },
  onViewStoreUpdate({ view }) {
    const viewState = get(view, 'state', Immutable.Map());
    const newState = viewState.map(state => state.titles.getIn(['tab', 'title'])).filter(v => v !== undefined);
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
