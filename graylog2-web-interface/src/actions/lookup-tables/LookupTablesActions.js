import Reflux from 'reflux';

const LookupTablesActions = Reflux.createActions({
  searchPaginated: { asyncResult: true },
  get: { asyncResult: true },
  create: { asyncResult: true },
  delete: { asyncResult: true },
});

export default LookupTablesActions;
