import Reflux from 'reflux';

const ConfigurationBundlesActions = Reflux.createActions({
  apply: { asyncResult: true },
  create: { asyncResult: true },
  delete: { asyncResult: true },
  export: { asyncResult: true },
  list: { asyncResult: true },
});

export default ConfigurationBundlesActions;
