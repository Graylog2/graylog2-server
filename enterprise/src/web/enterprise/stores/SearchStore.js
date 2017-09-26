import Reflux from 'reflux';
import Immutable from 'immutable';

import SearchActions from 'enterprise/actions/SearchActions';

export default Reflux.createStore({
  listenables: [SearchActions],
  state: {
    query: '',
    rangeType: 'relative',
    rangeParams: Immutable.Map({ relative: '300' }),
  },
  getInitialState() {
    return this.state;
  },
  query(query) {
    this.state.query = query;
    this.trigger(this.state);
  },
  rangeParams(key, value) {
    this.state.rangeParams = this.state.rangeParams.set(key, value);
    this.trigger(this.state);
  },
  rangeType(rangeType) {
    this.state.rangeType = rangeType;
    this.trigger(this.state);
  },
});