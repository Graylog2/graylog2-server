import Reflux from 'reflux';

const ConfigurationActions = Reflux.createActions({
  list: { asyncResult: true },
  listSearchesClusterConfig: { asyncResult: true },
  listMessageProcessorsConfig: { asyncResult: true },
  update: { asyncResult: true },
  updateMessageProcessorsConfig: { asyncResult: true },
});

export default ConfigurationActions;
