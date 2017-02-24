import Reflux from 'reflux';

const InputsActions = Reflux.createActions({
  list: { asyncResult: true },
  get: { asyncResult: true },
  getOptional: { asyncResult: true },
  create: { asyncResult: true },
  delete: { asyncResult: true },
  update: { asyncResult: true },
});

export default InputsActions;
