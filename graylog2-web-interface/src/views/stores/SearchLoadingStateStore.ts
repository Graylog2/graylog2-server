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

import { Store } from 'stores/StoreTypes';
import { singletonActions, singletonStore } from 'views/logic/singleton';

import { SearchActions } from './SearchStore';

export const SearchLoadingStateActions = singletonActions(
  'views.SearchLoadingState',
  () => Reflux.createActions(['loading', 'finished']),
);

type SearchLoadingStateStoreState = {
  isLoading: boolean;
};

export const SearchLoadingStateStore: Store<SearchLoadingStateStoreState> = singletonStore(
  'views.SearchLoadingState',
  () => Reflux.createStore({
    listenables: [SearchLoadingStateActions],
    isLoading: false,
    init() {
      SearchActions.execute.listen(this.loading);
      SearchActions.execute.completed.listen(this.finished);
    },
    getInitialState() {
      return this._state();
    },
    loading() {
      this.isLoading = true;
      this._trigger();
    },
    finished() {
      this.isLoading = false;
      this._trigger();
    },
    _state() {
      return {
        isLoading: this.isLoading,
      };
    },
    _trigger() {
      this.trigger(this._state());
    },
  }),
);
