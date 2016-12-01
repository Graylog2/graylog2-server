import Reflux from 'reflux';

const IndicesActions = Reflux.createActions({
  list: { asyncResult: true },
  listAll: { asyncResult: true },
  close: { asyncResult: true },
  delete: { asyncResult: true },
  multiple: { asyncResult: true },
  reopen: { asyncResult: true },
  subscribe: { asyncResult: false },
  unsubscribe: { asyncResult: false },
});

export default IndicesActions;
