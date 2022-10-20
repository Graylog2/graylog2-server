/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import Reflux from 'reflux';
import type * as Immutable from 'immutable';

import type { RefluxActions } from 'stores/StoreTypes';
import type { QueryId, TimeRange, TimeRangeTypes } from 'views/logic/queries/Query';
import type Query from 'views/logic/queries/Query';
import type ViewState from 'views/logic/views/ViewState';
import { singletonActions } from 'logic/singleton';

export type QueriesList = Immutable.OrderedMap<QueryId, Query>;

type QueriesActionsType = RefluxActions<{
  create: (query: Query, viewState: ViewState) => Promise<QueriesList>,
  duplicate: (queryId: QueryId) => Promise<QueriesList>,
  query: (queryId: QueryId, newQueryString: string) => Promise<QueriesList>,
  rangeType: (queryId: QueryId, rangeType: TimeRangeTypes) => Promise<QueriesList>,
  rangeParams: (queryId: QueryId, key: string, value: string | number) => Promise<QueriesList>,
  remove: (queryId: QueryId) => Promise<QueriesList>,
  timerange: (queryId: QueryId, newTimeRange: TimeRange) => Promise<QueriesList>,
  update: (queryId: QueryId, query: Query) => Promise<QueriesList>,
  forceUpdate: (queryId: QueryId, query: Query) => Promise<QueriesList>,
  setOrder: (newOrder: Immutable.OrderedSet<{ id: QueryId}>) => Promise<QueriesList>,
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
    forceUpdate: { asyncResult: true },
    setOrder: { asyncResult: true },
  }),
);
