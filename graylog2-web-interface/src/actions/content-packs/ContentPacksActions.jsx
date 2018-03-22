import Reflux from 'reflux';

const ContentPacksActions = Reflux.createActions({
  create: { asyncResult: true },
  list: { asyncResult: true },
  get: { asyncResult: true },
  delete: { asyncResult: true },
});

export default ContentPacksActions;
