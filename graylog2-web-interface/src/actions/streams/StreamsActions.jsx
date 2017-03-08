import Reflux from 'reflux';

const StreamsActions = Reflux.createActions({
  list: { asyncResult: true },
  create: { asyncResult: true },
  update: { asyncResult: true },
  delete: { asyncResult: true },
});

export default StreamsActions;
