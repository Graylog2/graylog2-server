import Reflux from 'reflux';

const EventDefinitionsActions = Reflux.createActions({
  listAll: { asyncResult: true },
  listPaginated: { asyncResult: true },
  get: { asyncResult: true },
  create: { asyncResult: true },
  update: { asyncResult: true },
  delete: { asyncResult: true },
  enable: { asyncResult: true },
  disable: { asyncResult: true },
});

export default EventDefinitionsActions;
