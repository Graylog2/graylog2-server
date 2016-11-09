import Reflux from 'reflux';

const AlertsActions = Reflux.createActions({
  list: { asyncResult: true },
  listPaginated: { asyncResult: true },
  listAllPaginated: { asyncResult: true },
  listAllStreams: { asyncResult: true },
});

export default AlertsActions;
