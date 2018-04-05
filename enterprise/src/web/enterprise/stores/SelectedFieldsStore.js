import Reflux from 'reflux';
import Immutable from 'immutable';

import CurrentViewStore from './CurrentViewStore';
import SelectedFieldsActions from '../actions/SelectedFieldsActions';

export default Reflux.createStore({
  listenables: [SelectedFieldsActions],
  selectedFields: new Immutable.Map(),
  selectedQuery: undefined,

  getInitialState() {
    return this._state();
  },

  add(queryId, field) {
    this.selectedFields = this.selectedFields.update(queryId, new Immutable.Set(), fields => fields.add(field));
    this._trigger();
  },
  remove(queryId, field) {
    this.selectedFields = this.selectedFields.update(queryId, new Immutable.Set(), fields => fields.remove(field));
    this._trigger();
  },
  set(queryId, fields) {
    this.selectedFields = this.selectedFields.set(queryId, new Immutable.Set(fields));
    this._trigger();
  },

  _state() {
    return this.selectedFields;
  },
  _trigger() {
    this.trigger(this._state());
  },
});
