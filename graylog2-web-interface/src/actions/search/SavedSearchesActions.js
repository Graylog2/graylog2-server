import Reflux from 'reflux';

const SavedSearchesActions = Reflux.createActions({
  create: { asyncResult: true },
  update: { asyncResult: true },
  list: { asyncResult: true },
  execute: { asyncResult: true },
  delete: { asyncResult: true },
});

export default SavedSearchesActions;
