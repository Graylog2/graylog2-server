import Reflux from 'reflux';

const LookupTableCachesActions = Reflux.createActions({
  searchPaginated: { asyncResult: true },
  reloadPage: { asyncResult: true },
  get: { asyncResult: true },
  create: { asyncResult: true },
  delete: { asyncResult: true },
  update: { asyncResult: true },
  getTypes: { asyncResult: true },
  validate: { asyncResult: true },
});

export default LookupTableCachesActions;
