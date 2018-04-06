import Reflux from 'reflux';
import Immutable from 'immutable';

import CurrentTitlesActions from '../actions/CurrentTitlesActions';
import CurrentViewStore from './CurrentViewStore';
import TitlesStore from './TitlesStore';
import TitlesActions from '../actions/TitlesActions';

export default Reflux.createStore({
  listenables: [CurrentTitlesActions],
  titles: new Immutable.Map(),
  selectedQuery: undefined,

  init() {
    this.listenTo(TitlesStore, this.onTitlesStoreChange, this.onTitlesStoreChange);
    this.listenTo(CurrentViewStore, this.onCurrentViewStoreChange, this.onCurrentViewStoreChange);
  },

  getInitialState() {
    return this._state();
  },

  onCurrentViewStoreChange(state) {
    if (this.selectedQuery !== state.selectedQuery) {
      this.selectedQuery = state.selectedQuery;
      this._trigger();
    }
  },

  onTitlesStoreChange(state) {
    this.titles = state;
    this._trigger();
  },

  set(type, id, title) {
    TitlesActions.set(this.selectedQuery, type, id, title);
  },

  _state() {
    return this.titles.get(this.selectedQuery, new Immutable.Map());
  },
  _trigger() {
    this.trigger(this._state());
  },
});
