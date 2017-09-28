import Reflux from 'reflux';
import uuidv4 from 'uuid/v4';

import QueriesActions from 'enterprise/actions/QueriesActions';

import hydration from './QueriesStore-hydration.json';

const answerQuery = (query) => {
  const response = {
    query,
    results: {
      messages: hydration.messages,
      histogram: hydration.histogram,
    },
  };
  return Promise.resolve(response);
};

export default Reflux.createStore({
  listenables: [QueriesActions],
  queries: {},
  getInitialState() {
    return this.queries;
  },
  create(query) {
    const id = uuidv4();
    this.queries[id] = { query };
    this._trigger();
    QueriesActions.create.promise(Promise.resolve(id));
  },
  update(id, query) {
    this.queries[id] = { query };
    this._trigger();
    const promise = answerQuery(query)
      .then((response) => {
        this.queries[id] = response;
        this._trigger();
      });
    QueriesActions.update.promise(promise);
  },
  remove(id) {
    delete this.queries[id];
    this._trigger();
  },
  _trigger() {
    this.trigger(this.queries);
  },
});