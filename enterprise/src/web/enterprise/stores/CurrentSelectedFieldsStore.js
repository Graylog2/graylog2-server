import Reflux from 'reflux';
import Immutable from 'immutable';

import CurrentSelectedFieldsActions from '../actions/CurrentSelectedFieldsActions';
import CurrentViewStore from './CurrentViewStore';
import SelectedFieldsStore from './SelectedFieldsStore';
import SelectedFieldsActions from '../actions/SelectedFieldsActions';

export default Reflux.createStore({
  listenables: [CurrentSelectedFieldsActions],
  selectedFields: new Immutable.Map(),
  selectedQuery: undefined,

  init() {
    this.listenTo(SelectedFieldsStore, this.onSelectedFieldsStoreChange, this.onSelectedFieldsStoreChange);
    this.listenTo(CurrentViewStore, this.onCurrentViewStoreChange, this.onCurrentViewStoreChange);
  },

  getInitialState() {
    return this._state();
  },

  onSelectedFieldsStoreChange(state) {
    this.selectedFields = state;
    this._trigger();
  },

  onCurrentViewStoreChange(state) {
    if (this.selectedQuery !== state.selectedQuery) {
      this.selectedQuery = state.selectedQuery;
      this._trigger();
    }
  },

  add(field) {
    SelectedFieldsActions.add(this.selectedQuery, field);
  },
  remove(field) {
    SelectedFieldsActions.remove(this.selectedQuery, field);
  },
  toggle(field) {
    if (this._state().contains(field)) {
      this.remove(field);
    } else {
      this.add(field);
    }
  },

  _state() {
    return this.selectedFields.get(this.selectedQuery, new Immutable.Set());
  },
  _trigger() {
    this.trigger(this._state());
  },

});
