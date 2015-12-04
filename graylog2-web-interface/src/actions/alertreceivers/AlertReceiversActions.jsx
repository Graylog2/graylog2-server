import Reflux from 'reflux';

const AlertReceiverActions = Reflux.createActions({
  'addReceiver': { asyncResult: true },
  'deleteReceiver': { asyncResult: true },
  'sendDummyAlert': { asyncResult: true },
});

export default AlertReceiverActions;
