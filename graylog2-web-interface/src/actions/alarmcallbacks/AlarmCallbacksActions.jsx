import Reflux from 'reflux';

const AlarmCallbacksActions = Reflux.createActions({
  delete: { asyncResult: true },
  list: { asyncResult: true },
  save: { asyncResult: true },
  update: { asyncResult: true },
});

export default AlarmCallbacksActions;
