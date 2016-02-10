import Reflux from 'reflux';

import RefreshActions from 'actions/tools/RefreshActions';

const RefreshStore = Reflux.createStore({
  listenables: [RefreshActions],
  interval: 5 * 1000,
  enabled: false,

  getInitialState() {
    return {
      interval: this.interval,
      enabled: this.enabled,
    };
  },

  changeInterval(newValue) {
    this.interval = newValue;
    this._update();
  },

  disable() {
    this.enabled = false;
    this._update();
  },

  enable() {
    this.enabled = true;
    this._update();
  },

  _update() {
    this.trigger(this.getInitialState());
  },
});

export default RefreshStore;
