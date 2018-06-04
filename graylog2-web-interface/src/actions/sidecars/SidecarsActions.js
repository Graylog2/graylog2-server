import Reflux from 'reflux';

const SidecarsActions = Reflux.createActions({
  listPaginated: { asyncResult: true },
  getSidecar: { asyncResult: true },
  getSidecarActions: { asyncResult: true },
  restartCollector: { asyncResult: true },
  assignConfigurations: { asyncResult: true },
});

export default SidecarsActions;
