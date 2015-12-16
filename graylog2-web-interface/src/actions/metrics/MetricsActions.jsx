import Reflux from 'reflux';

const MetricsActions = Reflux.createActions({
  'list': { asyncResult: true },
  'names': { asyncResult: true },
  'add': { asyncResult: true },
  'addGlobal': { asyncResult: true },
  'remove': { asyncResult: true },
  'removeGlobal': { asyncResult: true },
  'filter': { asyncResult: true },
});

export default MetricsActions;
