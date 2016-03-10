import Reflux from 'reflux';

const AlertsActions = Reflux.createActions({
  list: { asyncResult: true },
});

export default AlertsActions;
