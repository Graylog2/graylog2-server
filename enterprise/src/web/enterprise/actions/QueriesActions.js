import Reflux from 'reflux';

export default Reflux.createActions({
  create: { asyncResult: true },
  run: { asyncResult: true },
  jobStatus: { asyncResult: true },
  remove: { asyncResult: true },
});
