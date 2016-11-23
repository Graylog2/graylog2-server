import Reflux from 'reflux';

const IndexSetsActions = Reflux.createActions({
  list: { asyncResult: true },
  listPaginated: { asyncResult: true },
  get: { asyncResult: true },
  update: { asyncResult: true },
  create: { asyncResult: true },
});

export default IndexSetsActions;
