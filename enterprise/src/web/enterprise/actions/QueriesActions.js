import Reflux from 'reflux';

export const QueriesActions = Reflux.createActions({
  create: { asyncResult: true },
  load: {},
  query: {},
  rangeType: {},
  rangeParams: {},
  remove: {},
  update: {},
});
