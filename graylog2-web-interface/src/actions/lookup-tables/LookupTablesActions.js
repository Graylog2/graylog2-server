import Reflux from 'reflux';

const LookupTablesActions = Reflux.createActions({
  searchPaginated: { asyncResult: true },
  reloadPage: { asyncResult: true },
  get: { asyncResult: true },
  create: { asyncResult: true },
  delete: { asyncResult: true },
  update: { asyncResult: true },
  getErrors: { asyncResult: true },
});

export default LookupTablesActions;
