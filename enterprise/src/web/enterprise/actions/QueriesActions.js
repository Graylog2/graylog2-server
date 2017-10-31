import Reflux from 'reflux';

export default Reflux.createActions({
  create: { asyncResult: true },
  remove: { asyncResult: true },
});
