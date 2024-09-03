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
