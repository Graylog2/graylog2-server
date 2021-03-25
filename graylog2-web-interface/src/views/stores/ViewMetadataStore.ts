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

import type { Store } from 'stores/StoreTypes';
import { singletonStore } from 'views/logic/singleton';
import { ViewType } from 'views/logic/views/View';

import { ViewStore } from './ViewStore';

export type ViewMetaData = {
  activeQuery: string,
  description: string,
  id: string,
  summary: string,
  title: string,
  type: ViewType,
};

export type ViewMetadataStoreType = Store<ViewMetaData>;

// eslint-disable-next-line import/prefer-default-export
export const ViewMetadataStore: ViewMetadataStoreType = singletonStore(
  'views.ViewMetadata',
  () => Reflux.createStore({
    state: {},
    init() {
      this.listenTo(ViewStore, this.onViewStoreUpdate, this.onViewStoreUpdate);
    },
    getInitialState() {
      return this._state();
    },
    onViewStoreUpdate({ view, activeQuery }) {
      let newState;

      if (view) {
        const { id, title, description, summary, type } = view;

        newState = { id, title, description, summary, activeQuery, type };
      } else {
        newState = {};
      }

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
