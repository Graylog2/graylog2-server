// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import type { RefluxActions } from 'stores/StoreTypes';
import type { QueryId, TimeRange, TimeRangeTypes } from 'views/logic/queries/Query';
import Query from 'views/logic/queries/Query';
import ViewState from 'views/logic/views/ViewState';
import { singletonActions } from 'views/logic/singleton';

export type QueriesList = Immutable.OrderedMap<QueryId, Query>;

type QueriesActionsType = RefluxActions<{
  create: (Query, ViewState) => Promise<QueriesList>,
  duplicate: (QueryId) => Promise<QueriesList>,
  query: (QueryId, string) => Promise<QueriesList>,
  rangeType: (QueryId, TimeRangeTypes) => Promise<QueriesList>,
  rangeParams: (QueryId, string, string | number) => Promise<QueriesList>,
  remove: (QueryId) => Promise<QueriesList>,
  timerange: (QueryId, TimeRange) => Promise<QueriesList>,
  update: (QueryId, Query) => Promise<QueriesList>,
}>;

// eslint-disable-next-line import/prefer-default-export
export const QueriesActions: QueriesActionsType = singletonActions(
  'views.Queries',
  () => Reflux.createActions({
    create: { asyncResult: true },
    duplicate: { asyncResult: true },
    query: { asyncResult: true },
    rangeType: { asyncResult: true },
    rangeParams: { asyncResult: true },
    remove: { asyncResult: true },
    timerange: { asyncResult: true },
    update: { asyncResult: true },
  }),
);
