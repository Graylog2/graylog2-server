import Reflux from 'reflux';

const SystemJobsActions = Reflux.createActions({
  'list': { asyncResult: true },
});

export default SystemJobsActions;
