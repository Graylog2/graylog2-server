import Reflux from 'reflux';

const DeflectorActions = Reflux.createActions({
  'config': { asyncResult: true },
  'cycle': { asyncResult: true },
  'list': { asyncResult: true },
});

export default DeflectorActions;
