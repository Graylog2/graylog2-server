import Reflux from 'reflux';

export default Reflux.createActions({
  create: { asyncResult: true },
  load: { asyncResult: true },
  remove: { asyncResult: true },
  title: { asyncResult: true },
  update: { asyncResult: true },
});
