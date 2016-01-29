import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

const LdapStore = Reflux.createStore({
  sourceUrl: '/system/ldap/',
  ldapSettings: undefined,

  init() {
    this.loadSettings();
  },

  getInitialState() {
    return {ldapSettings: this.ldapSettings};
  },

  loadSettings() {
    const url = URLUtils.qualifyUrl(`${this.sourceUrl}/settings`);

    const promise = fetch('GET', url);
    promise.then(response => {
      this.ldapSettings = response;
      this.trigger({ldapSettings: response});
    });

    return promise;
  },
});

export default LdapStore;
