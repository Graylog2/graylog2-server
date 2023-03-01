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

import { singletonActions, singletonStore } from 'logic/singleton';
import type { Store } from 'stores/StoreTypes';
import type { SearchesConfig } from 'components/search/SearchConfig';
import { ConfigurationsActions } from 'stores/configurations/ConfigurationsStore';

type SearchConfigActionsType = {
  refresh: () => Promise<void>;
};

export const SearchConfigActions = singletonActions(
  'views.SearchConfig',
  () => Reflux.createActions<SearchConfigActionsType>({
    refresh: { asyncResult: true },
  }),
);

export type SearchConfigStoreState = {
  searchesClusterConfig: SearchesConfig;
};

export const SearchConfigStore: Store<SearchConfigStoreState> = singletonStore(
  'views.SearchConfig',
  () => Reflux.createStore({
    listenables: [SearchConfigActions],

    init() {
      this.refresh();
    },

    getInitialState() {
      return this._state();
    },

    refresh() {
      ConfigurationsActions.listSearchesClusterConfig().then((response) => {
        this.searchesClusterConfig = response;
        this._trigger();
      });
    },

    _state() {
      return {
        searchesClusterConfig: this.searchesClusterConfig,
      };
    },
    _trigger() {
      this.trigger(this._state());
    },
  }),
);
