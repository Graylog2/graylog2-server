import Reflux from 'reflux';

const SystemJobsActions = Reflux.createActions({
  list: { asyncResult: true },
  getJob: { asyncResult: true },
  cancelJob: { asyncResult: true },
});

export default SystemJobsActions;
