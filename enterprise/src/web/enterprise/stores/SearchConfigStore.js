import Reflux from 'reflux';

import CombinedProvider from 'injection/CombinedProvider';

const { ConfigurationActions } = CombinedProvider.get('Configuration');
const { ConfigurationStore } = CombinedProvider.get('Configurations');

export const SearchConfigStore = Reflux.createStore({
  init() {
    ConfigurationActions.listSearchesClusterConfig().then((response) => {
      this.searchesClusterConfig = response;
      this._trigger();
    });
  },

  getInitialState() {
    return this._state();
  },

  _state() {
    return {
      searchesClusterConfig: this.searchesClusterConfig,
    };
  },
  _trigger() {
    this.trigger(this._state());
  },
});
