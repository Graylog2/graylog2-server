import Reflux from 'reflux';
import Immutable from 'immutable';

import CurrentViewActions from 'enterprise/actions/CurrentViewActions';

export default Reflux.createStore({
  listenables: [CurrentViewActions],
  state: new Immutable.Map(),

  getInitialState() {
    return this.state.toJS();
  },

  selectView(viewId) {
    this.state = this.state.set('selectedView', viewId);
    this._trigger();
  },
  selectQuery(queryId) {
    this.state = this.state.set('selectedQuery', queryId);
    this._trigger();
  },
  selectSearchJob(jobId) {
    this.state = this.state.set('selectedSearchJob', jobId);
    this._trigger();
  },
  currentWidgetMapping(widgetMapping) {
    this.state = this.state.set('widgetMapping', widgetMapping);
    this._trigger();
  },

  _trigger() {
    this.trigger(this.state.toJS());
  },
});