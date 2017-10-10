import Reflux from 'reflux';

const DashboardsActions = Reflux.createActions({
  create: { asyncResult: true },
  delete: { asyncResult: true },
  get: { asyncResult: true },
  list: { asyncResult: true },
  update: { asyncResult: true },
  updatePositions: { asyncResult: true },
});

export default DashboardsActions;
