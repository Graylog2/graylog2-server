// @flow strict
import Reflux from 'reflux';
import type { QueryId } from 'enterprise/logic/queries/Query';
import Query from 'enterprise/logic/queries/Query';
import ViewState from 'enterprise/logic/views/ViewState';

type QueriesActionsType = {
  create: (Query, ViewState) => Promise<*>,
  duplicate: (QueryId) => Promise<Query>,
  query: (QueryId, string) => Promise<*>,
  rangeType: (QueryId) => Promise<*>,
  rangeParams: (QueryId) => Promise<*>,
  remove: (QueryId) => Promise<*>,
  timerange: (QueryId) => Promise<*>,
  update: (QueryId, Query) => Promise<*>,
};

// eslint-disable-next-line import/prefer-default-export
export const QueriesActions: QueriesActionsType = Reflux.createActions({
  create: { asyncResult: true },
  duplicate: { asyncResult: true },
  query: { asyncResult: true },
  rangeType: { asyncResult: true },
  rangeParams: { asyncResult: true },
  remove: { asyncResult: true },
  timerange: { asyncResult: true },
  update: { asyncResult: true },
});
