import Reflux from 'reflux';

const AlertReceiverActions = Reflux.createActions({
  'addReceiver': { asyncResult: true },
  'deleteReceiver': { asyncResult: true},
});

export default AlertReceiverActions;
