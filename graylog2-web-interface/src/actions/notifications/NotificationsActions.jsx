import Reflux from 'reflux';

const NotificationsActions = Reflux.createActions({
  delete: { asyncResult: true },
  list: { asyncResult: true },
});

export default NotificationsActions;
