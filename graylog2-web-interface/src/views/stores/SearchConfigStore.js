import Reflux from 'reflux';

import CombinedProvider from 'injection/CombinedProvider';
import { singletonActions, singletonStore } from 'views/logic/singleton';

const { ConfigurationActions } = CombinedProvider.get('Configuration');

export const SearchConfigActions = singletonActions(
  'views.SearchConfig',
  () => Reflux.createActions({
    refresh: { asyncResult: true },
  }),
);

// eslint-disable-next-line import/prefer-default-export
export const SearchConfigStore = singletonStore(
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
      ConfigurationActions.listSearchesClusterConfig().then((response) => {
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
