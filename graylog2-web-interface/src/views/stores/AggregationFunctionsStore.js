import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

const functionsUrl = URLUtils.qualifyUrl('/plugins/org.graylog.plugins.enterprise/functions');

export default Reflux.createStore({
  init() {
    this.refresh();
  },

  getInitialState() {
    return this._state();
  },

  refresh() {
    fetch('GET', functionsUrl).then((response) => {
      this.functions = response;
      this._trigger();
    });
  },
  _state() {
    return this.functions;
  },
  _trigger() {
    this.trigger(this._state());
  },
});
