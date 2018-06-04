import Reflux from 'reflux';

const CollectorsActions = Reflux.createActions({
  getCollector: { asyncResult: true },
  all: { asyncResult: true },
  list: { asyncResult: true },
  create: { asyncResult: true },
  update: { asyncResult: true },
  delete: { asyncResult: true },
  copy: { asyncResult: true },
  validate: { asyncResult: true },
});

export default CollectorsActions;
