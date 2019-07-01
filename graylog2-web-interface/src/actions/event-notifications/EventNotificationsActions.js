import Reflux from 'reflux';

const EventNotificationsActions = Reflux.createActions({
  list: { asyncResult: true },
  get: { asyncResult: true },
  create: { asyncResult: true },
  update: { asyncResult: true },
  delete: { asyncResult: true },
});

export default EventNotificationsActions;
