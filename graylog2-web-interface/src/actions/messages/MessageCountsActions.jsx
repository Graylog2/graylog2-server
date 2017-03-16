import Reflux from 'reflux';

const MessageCountsActions = Reflux.createActions({
  total: { asyncResult: true },
});

export default MessageCountsActions;
