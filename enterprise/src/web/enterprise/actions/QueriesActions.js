import Reflux from 'reflux';

// eslint-disable-next-line import/prefer-default-export
export const QueriesActions = Reflux.createActions({
  create: { asyncResult: true },
  load: { asyncResult: true },
  query: { asyncResult: true },
  rangeType: { asyncResult: true },
  rangeParams: { asyncResult: true },
  remove: { asyncResult: true },
  timerange: { asyncResult: true },
  update: { asyncResult: true },
});
