import Reflux from 'reflux';
import Immutable from 'immutable';

import TitlesActions from '../actions/TitlesActions';

export default Reflux.createStore({
  listenables: [TitlesActions],

  titles: new Immutable.Map(),

  load(queryId, titles) {
    this.titles = this.titles.set(queryId, Immutable.fromJS(titles));
    this._trigger();
  },

  getInitialState() {
    return this.titles;
  },

  set(queryId, type, id, title) {
    this.titles = this.titles.setIn([queryId, type, id], title);
    this._trigger();
  },

  _trigger() {
    this.trigger(this.titles);
  },
});
