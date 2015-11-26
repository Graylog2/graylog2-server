import Reflux from 'reflux';

const InputsActions = Reflux.createActions({
  'list': {asyncResult: true},
  'get': {asyncResult: true},
});

export default InputsActions;
