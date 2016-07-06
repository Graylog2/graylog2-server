import Reflux from 'reflux';

const DecoratorsActions = Reflux.createActions({
  create: { asyncResult: true },
  list: { asyncResult: true },
  available: { asyncResult: true },
  remove: { asyncResult: true },
});

export default DecoratorsActions;
