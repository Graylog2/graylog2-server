import Reflux from 'reflux';

const LookupTableCachesActions = Reflux.createActions({

  searchPaginated: { asyncResult: true },
  get: { asyncResult: true },
  create: { asyncResult: true },
  delete: { asyncResult: true },

});

export default LookupTableCachesActions;
