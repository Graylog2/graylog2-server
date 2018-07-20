import Reflux from 'reflux';

const CatalogActions = Reflux.createActions({
  showEntityIndex: { asyncResult: true },
  getSelectedEntities: { asyncResult: true },
});

export default CatalogActions;
