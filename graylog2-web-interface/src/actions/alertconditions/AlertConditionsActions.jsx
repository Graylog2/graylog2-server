import Reflux from 'reflux';

const AlertConditionsActions = Reflux.createActions({
  available: { asyncResult: true },
  delete: { asyncResult: true },
  list: { asyncResult: true },
  listAll: { asyncResult: true },
  save: { asyncResult: true },
  update: { asyncResult: true },
  get: { asyncResult: true },
});

export default AlertConditionsActions;
