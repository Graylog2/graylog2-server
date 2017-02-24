import Reflux from 'reflux';

const NodesActions = Reflux.createActions({
  list: { asyncResult: true },
});

export default NodesActions;
