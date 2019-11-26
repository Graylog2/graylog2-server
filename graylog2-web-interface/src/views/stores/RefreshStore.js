// @flow strict
import Reflux from 'reflux';
import SearchActions from 'views/actions/SearchActions';
import { singletonActions, singletonStore } from 'views/logic/singleton';

type RefreshActionsType = {
  enable: () => void,
  disable: () => void,
  setInterval: (number) => void,
};

export const RefreshActions: RefreshActionsType = singletonActions(
  'views.Refresh',
  () => Reflux.createActions([
    'enable',
    'disable',
    'setInterval',
  ]),
);

export const RefreshStore = singletonStore(
  'views.Refresh',
  () => Reflux.createStore({
    listenables: [RefreshActions],

    refreshConfig: {},

    intervalId: undefined,

    init() {
      this.refreshConfig = {
        enabled: false,
        interval: 1000,
      };
    },

    getInitialState() {
      return this.refreshConfig;
    },

    setInterval(interval: number) {
      this.refreshConfig.interval = interval;
      this.refreshConfig.enabled = true;

      if (this.intervalId) {
        clearInterval(this.intervalId);
        this.intervalId = undefined;
      }
      this.intervalId = setInterval(SearchActions.executeWithCurrentState, this.refreshConfig.interval);
      this._trigger();
    },

    enable() {
      this.refreshConfig.enabled = true;
      if (!this.intervalId) {
        this.intervalId = setInterval(SearchActions.executeWithCurrentState, this.refreshConfig.interval);
      }
      this._trigger();
    },

    disable() {
      this.refreshConfig.enabled = false;
      if (this.intervalId) {
        clearInterval(this.intervalId);
        this.intervalId = undefined;
      }
      this._trigger();
    },

    _trigger() {
      this.trigger(this.refreshConfig);
    },
  }),
);
