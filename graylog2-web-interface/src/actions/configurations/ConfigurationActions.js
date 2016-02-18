import Reflux from 'reflux';

const ConfigurationActions = Reflux.createActions({
  'list': { asyncResult: true },
  'listSearchesClusterConfig': { asyncResult: true },
  'update': { asyncResult: true },
});

export default ConfigurationActions;
