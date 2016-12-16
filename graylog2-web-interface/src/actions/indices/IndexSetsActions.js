import Reflux from 'reflux';

const IndexSetsActions = Reflux.createActions({
  list: { asyncResult: true },
  listPaginated: { asyncResult: true },
  get: { asyncResult: true },
  update: { asyncResult: true },
  create: { asyncResult: true },
  delete: { asyncResult: true },
  setDefault: { asyncResult: true },
});

export default IndexSetsActions;
