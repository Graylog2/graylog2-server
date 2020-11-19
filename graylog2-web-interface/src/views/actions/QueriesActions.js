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
