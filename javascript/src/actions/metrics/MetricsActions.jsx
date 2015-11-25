import Reflux from 'reflux';

const MetricsActions = Reflux.createActions({
  'list': { asyncResult: true },
  'names': { asyncResult: true },
  'add': { asyncResult: true },
  'remove': { asyncResult: true },
});

export default MetricsActions;
