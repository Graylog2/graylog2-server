import Reflux from 'reflux';

const InputTypesActions = Reflux.createActions({
  list: { asyncResult: true },
  get: { asyncResult: true },
});

export default InputTypesActions;
