import Reflux from 'reflux';

const AlertNotificationsActions = Reflux.createActions({
  available: { asyncResult: true },
  listAll: { asyncResult: true },
  testAlert: { asyncResult: true },
});

export default AlertNotificationsActions;
