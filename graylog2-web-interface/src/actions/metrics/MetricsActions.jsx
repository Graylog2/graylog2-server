import Reflux from 'reflux';

const MetricsActions = Reflux.createActions({
  add: { asyncResult: true },
  addGlobal: { asyncResult: true },
  clear: { asyncResult: true },
  filter: { asyncResult: true },
  list: { asyncResult: true },
  names: { asyncResult: true },
  remove: { asyncResult: true },
  removeGlobal: { asyncResult: true },
});

export default MetricsActions;
