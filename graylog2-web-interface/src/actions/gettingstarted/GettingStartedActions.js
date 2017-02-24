import Reflux from 'reflux';

const GettingStartedActions = Reflux.createActions({
  getStatus: { asyncResult: true },
  dismiss: { asyncResult: true },
});

export default GettingStartedActions;
