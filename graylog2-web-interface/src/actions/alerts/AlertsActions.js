import Reflux from 'reflux';

const AlertsActions = Reflux.createActions({
  get: { asyncResult: true },
  list: { asyncResult: true },
  listPaginated: { asyncResult: true },
  listAllPaginated: { asyncResult: true },
  listAllStreams: { asyncResult: true },
});

export default AlertsActions;
