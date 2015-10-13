import Reflux from 'reflux';

const StreamActions = Reflux.createActions({
  'list': { asyncResult: true },
  'create': { asyncResult: true},
  'update': { asyncResult: true},
  'delete': { asyncResult: true},
});

export default StreamActions;
