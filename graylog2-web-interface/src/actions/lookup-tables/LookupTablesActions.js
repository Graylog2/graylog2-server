import Reflux from 'reflux';

const LookupTablesActions = Reflux.createActions({
  searchPaginated: { asyncResult: true },
  reloadPage: { asyncResult: true },
  get: { asyncResult: true },
  create: { asyncResult: true },
  delete: { asyncResult: true },
  update: { asyncResult: true },
  getErrors: { asyncResult: true },
  lookup: { asyncResult: true },
  purgeKey: { asyncResult: true },
  purgeAll: { asyncResult: true },
  validate: { asyncResult: true },
});

export default LookupTablesActions;
