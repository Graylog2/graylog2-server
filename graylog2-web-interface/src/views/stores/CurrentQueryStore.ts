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
import { isEqual } from 'lodash';

import { Store } from 'stores/StoreTypes';
import { singletonStore } from 'views/logic/singleton';
import Query from 'views/logic/queries/Query';

import { QueriesStore } from './QueriesStore';
import { ViewStore } from './ViewStore';

// eslint-disable-next-line import/prefer-default-export
export const CurrentQueryStore: Store<Query> = singletonStore(
  'views.CurrentQuery',
  () => Reflux.createStore({
    init() {
      this.listenTo(ViewStore, this.onViewStoreUpdate, this.onViewStoreUpdate);
      this.listenTo(QueriesStore, this.onQueriesStoreUpdate, this.onQueriesStoreUpdate);
    },
    getInitialState() {
      return this._state();
    },
    onQueriesStoreUpdate(queries) {
      this.queries = queries;

      if (this.activeQuery) {
        const newQuery = queries.get(this.activeQuery);

        if (!isEqual(newQuery, this.query)) {
          this.query = newQuery;
          this._trigger();
        }
      }
    },
    onViewStoreUpdate({ activeQuery }) {
      if (!isEqual(activeQuery, this.activeQuery)) {
        this.activeQuery = activeQuery;
        this.query = this.queries.get(activeQuery);
        this._trigger();
      }
    },
    _state() {
      return this.query;
    },
    _trigger() {
      this.trigger(this._state());
    },
  }),
);
