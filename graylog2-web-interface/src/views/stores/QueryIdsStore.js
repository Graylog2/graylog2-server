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

import { singletonStore } from 'views/logic/singleton';

import { QueriesStore } from './QueriesStore';

// eslint-disable-next-line import/prefer-default-export
export const QueryIdsStore = singletonStore(
  'views.QueryIds',
  () => Reflux.createStore({
    state: {},
    init() {
      this.listenTo(QueriesStore, this.onQueriesStoreUpdate, this.onQueriesStoreUpdate);
    },
    getInitialState() {
      return this._state();
    },
    onQueriesStoreUpdate(queries) {
      const newState = queries.keySeq().toList();

      if (!isEqual(this.state, newState)) {
        this.state = newState;
        this._trigger();
      }
    },
    _state() {
      return this.state;
    },
    _trigger() {
      this.trigger(this._state());
    },
  }),
);
