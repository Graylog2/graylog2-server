import Reflux from 'reflux';

const CollectorConfigurationsActions = Reflux.createActions({
  all: { asyncResult: true },
  list: { asyncResult: true },
  getConfiguration: { asyncResult: true },
  createConfiguration: { asyncResult: true },
  updateConfiguration: { asyncResult: true },
  renderPreview: { asyncResult: true },
  copyConfiguration: { asyncResult: true },
  delete: { asyncResult: true },
  validate: { asyncResult: true },
});

export default CollectorConfigurationsActions;
