import Reflux from 'reflux';

const CatalogActions = Reflux.createActions({
  showEntityIndex: { asyncResult: true },
});

export default CatalogActions;
