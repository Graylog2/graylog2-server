import Reflux from 'reflux';

const AlertsActions = Reflux.createActions({
  listPaginated: { asyncResult: true },
  listAllStreams: { asyncResult: true },
});

export default AlertsActions;
