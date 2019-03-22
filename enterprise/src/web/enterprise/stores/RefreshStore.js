// @flow strict
import Reflux from 'reflux';
import SearchActions from 'enterprise/actions/SearchActions';

type RefreshActionsType = {
  enable: () => void,
  disable: () => void,
  /* eslint-disable-next-line no-undef */
  setInterval: (number) => void,
};

export const RefreshActions: RefreshActionsType = Reflux.createActions([
  'enable',
  'disable',
  'setInterval',
]);

export const RefreshStore = Reflux.createStore({
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
});
