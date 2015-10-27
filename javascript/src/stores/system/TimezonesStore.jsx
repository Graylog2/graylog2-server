import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

const TimezonesStore = Reflux.createStore({
  URL: URLUtils.qualifyUrl('/system/timezones'),
  timezones: undefined,

  init() {
    fetch('GET', this.URL).then((response) => {
      this.timezones = response.timezones;
      this.trigger({timezones: this.timezones});
    });
  },
  getInitialState() {
    return {timezones: this.timezones};
  },
});

export default TimezonesStore;
