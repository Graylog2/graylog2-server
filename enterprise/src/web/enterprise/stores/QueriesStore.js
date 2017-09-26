import Reflux from 'reflux';
import uuidv4 from 'uuid/v4';

import QueriesActions from 'enterprise/actions/QueriesActions';

export default Reflux.createStore({
  listenables: [QueriesActions],
  queries: {},
  create(query) {
    const id = uuidv4();
    this.queries[id] = query;
    this._trigger();
    return id;
  },
  update(id, query) {
    this.queries[id] = query;
    this._trigger();
  },
  remove(id) {
    delete this.queries[id];
    this._trigger();
  },
  _trigger() {
    this.trigger(this.queries);
  },
});