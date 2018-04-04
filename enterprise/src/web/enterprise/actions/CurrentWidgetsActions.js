import Reflux from 'reflux';

export default Reflux.createActions({
  duplicate: { asyncResult: true},
  remove: { asyncResult: true},
  updateConfig: { asyncResult: true},
});
