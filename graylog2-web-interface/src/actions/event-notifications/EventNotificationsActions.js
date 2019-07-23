import Reflux from 'reflux';

const EventNotificationsActions = Reflux.createActions({
  listAll: { asyncResult: true },
  listAllLegacyTypes: { asyncResult: true },
  listPaginated: { asyncResult: true },
  get: { asyncResult: true },
  create: { asyncResult: true },
  update: { asyncResult: true },
  delete: { asyncResult: true },
});

export default EventNotificationsActions;
