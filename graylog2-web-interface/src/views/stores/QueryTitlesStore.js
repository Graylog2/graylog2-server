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
import Immutable from 'immutable';
import { get, isEqual } from 'lodash';

import { singletonStore } from 'views/logic/singleton';

import { ViewStore } from './ViewStore';

// eslint-disable-next-line import/prefer-default-export
export const QueryTitlesStore = singletonStore(
  'views.QueryTitles',
  () => Reflux.createStore({
    init() {
      this.listenTo(ViewStore, this.onViewStoreUpdate, this.onViewStoreUpdate);
    },
    getInitialState() {
      return this._state();
    },
    onViewStoreUpdate({ view }) {
      const viewState = get(view, 'state', Immutable.Map());
      const newState = viewState.map((state) => state.titles.getIn(['tab', 'title'])).filter((v) => v !== undefined);

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
