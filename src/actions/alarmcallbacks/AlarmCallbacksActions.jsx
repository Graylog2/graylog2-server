import Reflux from 'reflux';

const AlarmCallbacksActions = Reflux.createActions({
  'available': { asyncResult: true },
  'delete': { asyncResult: true },
  'list': { asyncResult: true },
  'save': { asyncResult: true },
  'update': { asyncResult: true },
});

export default AlarmCallbacksActions;
