import Reflux from 'reflux';

const AlarmCallbackHistoryActions = Reflux.createActions({
  list: { asyncResult: true },
});

export default AlarmCallbackHistoryActions;
