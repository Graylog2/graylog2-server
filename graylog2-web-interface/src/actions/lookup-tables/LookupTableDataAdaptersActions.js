import Reflux from 'reflux';

const LookupTableDataAdaptersActions = Reflux.createActions({

  searchPaginated: { asyncResult: true },
  get: { asyncResult: true },
  create: { asyncResult: true },
  delete: { asyncResult: true },

});

export default LookupTableDataAdaptersActions;
