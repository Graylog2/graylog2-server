import Reflux from 'reflux';

const ConfigurationVariableActions = Reflux.createActions({
  all: { asyncResult: true },
  save: { asyncResult: true },
  delete: { asyncResult: true },
  validate: { asyncResult: true },
});

export default ConfigurationVariableActions;
