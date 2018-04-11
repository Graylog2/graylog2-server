import Reflux from 'reflux';
import Immutable from 'immutable';

import WidgetFilterActions from '../actions/WidgetFilterActions';

export default Reflux.createStore({
  listenables: [WidgetFilterActions],
  filters: new Immutable.Map(),

  getInitialState() {
    return this.filters;
  },

  change(id, filter) {
    this.filters = this.filters.set(id, filter);
    this._trigger();
  },

  _trigger() {
    this.trigger(this.filters);
  },
});
