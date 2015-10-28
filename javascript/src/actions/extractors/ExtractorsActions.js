import Reflux from 'reflux';

const ExtractorsActions = Reflux.createActions({
  list: {asyncResult: true},
});

export default ExtractorsActions;
