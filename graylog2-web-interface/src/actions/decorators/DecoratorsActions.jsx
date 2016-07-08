import Reflux from 'reflux';

const DecoratorsActions = Reflux.createActions({
  available: { asyncResult: true },
  create: { asyncResult: true },
  list: { asyncResult: true },
  remove: { asyncResult: true },
  update: { asyncResult: true },
});

export default DecoratorsActions;
