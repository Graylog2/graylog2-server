import Reflux from 'reflux';

const AlertsActions = Reflux.createActions({
  list: { asyncResult: true },
  listPaginated: { asyncResult: true },
  listAllStreams: { asyncResult: true },
});

export default AlertsActions;
