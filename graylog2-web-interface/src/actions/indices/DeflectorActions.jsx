import Reflux from 'reflux';

const DeflectorActions = Reflux.createActions({
  cycle: { asyncResult: true },
  list: { asyncResult: true },
});

export default DeflectorActions;
