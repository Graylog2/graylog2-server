import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

const LdapStore = Reflux.createStore({
  ldapSettings: undefined,

  init() {
    this.loadSettings();
  },

  getInitialState() {
    return {ldapSettings: this.ldapSettings};
  },

  loadSettings() {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.LdapController.info().url);

    const promise = fetch('GET', url);
    promise.then(response => {
      this.ldapSettings = response;
      this.trigger({ldapSettings: response});
    });

    return promise;
  },
});

export default LdapStore;
