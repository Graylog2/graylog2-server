import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

const LdapStore = Reflux.createStore({
  ldapSettings: undefined,
  init() {
    this.loadSettings().then((response) => {
      this.trigger({ldapSettings: response});
      this.system = response;
    });
  },

  getInitialState() {
    return {ldapSettings: this.ldapSettings};
  },

  loadSettings() {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.LdapController.info().url);

    return fetch('GET', url);
  },
});

export default LdapStore;
