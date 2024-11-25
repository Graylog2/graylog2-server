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

import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import { singletonStore, singletonActions } from 'logic/singleton';

export type Plugin = {
  unique_id: string,
  required_version: string,
  author: string,
  required_capabilities: Array<string>,
  name: string,
  description: string,
  version: string,
  url: string,
};

export type ActionsType = {
  list: () => Promise<Array<Plugin>>,
};

export const SystemPluginsActions = singletonActions('SystemPlugins', () => Reflux.createActions<ActionsType>({
  list: { asyncResult: true },
}));

type SystemPluginsStoreState = {
  plugins: Array<Plugin>,
};

const SystemPluginsStore = singletonStore('SystemPlugins', () => Reflux.createStore<SystemPluginsStoreState>({
  listenables: [SystemPluginsActions],
  sourceUrl: '/system/plugins',
  plugins: undefined,

  getInitialState() {
    return this.getState();
  },

  getState() {
    return {
      plugins: this.plugins,
    };
  },

  propagateUpdate() {
    this.trigger(this.getState());
  },

  list() {
    const promise = fetch('GET', qualifyUrl(this.sourceUrl))
      .then((response) => {
        this.plugins = response.plugins;
        this.propagateUpdate();

        return response;
      });

    SystemPluginsActions.list.promise(promise);
  },
}));

export default SystemPluginsStore;
