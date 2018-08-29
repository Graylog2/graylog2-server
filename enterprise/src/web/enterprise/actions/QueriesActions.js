import Reflux from 'reflux';

// eslint-disable-next-line import/prefer-default-export
export const QueriesActions = Reflux.createActions({
  create: { asyncResult: true },
  load: {},
  query: {},
  rangeType: {},
  rangeParams: {},
  remove: {},
  update: {},
});
